package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.PantryConsumptionEvent;
import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ProductDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GuestSessionService {

  private static final Duration SESSION_TTL = Duration.ofHours(12);
  private final Map<String, GuestPantryState> sessions = new ConcurrentHashMap<>();
  private final OpenFoodFactsService openFoodFactsService;

  public GuestSessionService(OpenFoodFactsService openFoodFactsService) {
    this.openFoodFactsService = openFoodFactsService;
  }

  public String createSession() {
    cleanupExpiredSessions();
    String token = "guest-" + UUID.randomUUID();
    sessions.put(token, new GuestPantryState());
    return token;
  }

  public void clearSession(String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    sessions.remove(token);
  }

  public boolean isGuestToken(String token) {
    return token != null && token.startsWith("guest-");
  }

  public void authenticate(String token) {
    getState(token);
  }

  public List<PantryItem> getAll(String token) {
    GuestPantryState state = getState(token);
    List<PantryItem> items = new ArrayList<>();
    for (PantryItem item : state.items.values()) {
      if (item.getCount() != null && item.getCount() > 0) {
        items.add(cloneItem(item));
      }
    }
    items.sort(Comparator.comparing(PantryItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
    return items;
  }

  public PantryItem addItem(String token, PantryItemPostDTO pantryItemPostDTO) {
    GuestPantryState state = getState(token);

    String barcode = normalizeBlank(pantryItemPostDTO.getBarcode());
    String name = normalizeBlank(pantryItemPostDTO.getName());
    Integer count = pantryItemPostDTO.getCount();
    Double kcalPerPackage = pantryItemPostDTO.getKcalPerPackage();

    ProductDTO enriched = null;
    if (barcode != null) {
      try {
        enriched = openFoodFactsService.lookupByBarcode(barcode);
      } catch (ResponseStatusException ignored) {
        // Keep demo mode forgiving. We'll still allow a manual add when enough fields exist.
      }
    }

    if (name == null && enriched != null) {
      name = normalizeBlank(enriched.getName());
    }

    if (name == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name or barcode must be provided");
    }

    PantryItem item = new PantryItem();
    item.setId(state.nextItemId++);
    item.setBarcode(barcode != null ? barcode : (enriched != null ? normalizeBlank(enriched.getBarcode()) : null));
    item.setName(name);
    item.setBrand(enriched != null ? normalizeBlank(enriched.getBrand()) : null);
    item.setKcalPer100(enriched != null ? enriched.getKcalPer100() : null);
    item.setPackageQuantity(enriched != null ? enriched.getPackageQuantity() : null);
    item.setPackageUnit(enriched != null ? enriched.getPackageUnit() : null);
    item.setKcalPerPackage(kcalPerPackage != null ? kcalPerPackage : (enriched != null ? enriched.getKcalPerPackage() : null));
    item.setCount((count == null || count < 1) ? 1 : count);
    item.setCreatedAt(Instant.now());

    state.items.put(item.getId(), item);
    state.touch();
    return cloneItem(item);
  }

  public PantryItem consumeOne(String token, Long id) {
    GuestPantryState state = getState(token);
    PantryItem item = state.items.get(id);
    if (item == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry item not found");
    }

    int count = item.getCount() == null ? 1 : item.getCount();
    if (count <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is already consumed");
    }

    PantryConsumptionEvent ev = new PantryConsumptionEvent();
    ev.setPantryItemId(item.getId());
    ev.setBarcode(item.getBarcode());
    ev.setItemName(item.getName());
    ev.setKcal(item.getKcalPerPackage());
    ev.setConsumedAt(Instant.now());
    state.events.add(ev);

    item.setLastConsumedAt(Instant.now());
    if (count == 1) {
      item.setCount(0);
      if (item.getConsumedAt() == null) {
        item.setConsumedAt(Instant.now());
      }
    } else {
      item.setCount(count - 1);
    }

    state.touch();
    return cloneItem(item);
  }

  public void delete(String token, Long id) {
    GuestPantryState state = getState(token);
    PantryItem removed = state.items.remove(id);
    if (removed == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry item not found");
    }
    state.touch();
  }

  public Double setIdealDailyKcal(String token, Double idealDailyKcal) {
    if (idealDailyKcal == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idealDailyKcal must be provided");
    }
    if (idealDailyKcal <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idealDailyKcal must be > 0");
    }
    GuestPantryState state = getState(token);
    state.idealDailyKcal = idealDailyKcal;
    state.touch();
    return state.idealDailyKcal;
  }

  public PantryService.Stats computeStats(String token, LocalDate fromDate) {
    if (fromDate == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date is required");
    }

    GuestPantryState state = getState(token);
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    long days = ChronoUnit.DAYS.between(fromDate, today) + 1;
    if (days < 1) {
      days = 1;
    }

    Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    double totalConsumed = 0.0;
    long unknown = 0;
    for (PantryConsumptionEvent ev : state.events) {
      if (ev.getConsumedAt() == null || ev.getConsumedAt().isBefore(fromInstant)) {
        continue;
      }
      if (ev.getKcal() == null) {
        unknown++;
      } else {
        totalConsumed += ev.getKcal();
      }
    }

    double avgDaily = totalConsumed / (double) days;

    double totalOwned = 0.0;
    for (PantryItem item : state.items.values()) {
      if (item.getCount() != null && item.getCount() > 0 && item.getKcalPerPackage() != null) {
        totalOwned += item.getKcalPerPackage() * item.getCount();
      }
    }

    PantryService.Stats stats = new PantryService.Stats();
    stats.days = days;
    stats.totalCaloriesOwned = totalOwned;
    stats.totalCaloriesConsumed = totalConsumed;
    stats.avgDailyCaloriesConsumed = avgDaily;
    stats.idealDailyKcal = state.idealDailyKcal;
    stats.avgMinusIdeal = state.idealDailyKcal == null ? null : (avgDaily - state.idealDailyKcal);
    stats.unknownKcalEvents = unknown;
    return stats;
  }

  private GuestPantryState getState(String token) {
    cleanupExpiredSessions();
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing token");
    }
    GuestPantryState state = sessions.get(token);
    if (state == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid guest session");
    }
    state.touch();
    return state;
  }

  private void cleanupExpiredSessions() {
    Instant cutoff = Instant.now().minus(SESSION_TTL);
    sessions.entrySet().removeIf(entry -> entry.getValue().lastTouchedAt.isBefore(cutoff));
  }

  private static String normalizeBlank(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static PantryItem cloneItem(PantryItem src) {
    PantryItem item = new PantryItem();
    item.setId(src.getId());
    item.setBarcode(src.getBarcode());
    item.setName(src.getName());
    item.setBrand(src.getBrand());
    item.setCount(src.getCount());
    item.setKcalPer100(src.getKcalPer100());
    item.setPackageQuantity(src.getPackageQuantity());
    item.setPackageUnit(src.getPackageUnit());
    item.setKcalPerPackage(src.getKcalPerPackage());
    item.setCreatedAt(src.getCreatedAt());
    item.setConsumedAt(src.getConsumedAt());
    item.setLastConsumedAt(src.getLastConsumedAt());
    return item;
  }

  private static final class GuestPantryState {
    private final Map<Long, PantryItem> items = new LinkedHashMap<>();
    private final List<PantryConsumptionEvent> events = new ArrayList<>();
    private long nextItemId = 1L;
    private Double idealDailyKcal;
    private Instant lastTouchedAt = Instant.now();

    private void touch() {
      lastTouchedAt = Instant.now();
    }
  }
}
