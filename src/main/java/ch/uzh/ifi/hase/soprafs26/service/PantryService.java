package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.entity.PantryConsumptionEvent;
import ch.uzh.ifi.hase.soprafs26.entity.PantrySettings;
import ch.uzh.ifi.hase.soprafs26.repository.PantryConsumptionEventRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PantryItemRepository;
import ch.uzh.ifi.hase.soprafs26.repository.PantrySettingsRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PantryService {

  private final PantryItemRepository pantryItemRepository;
  private final PantryConsumptionEventRepository pantryConsumptionEventRepository;
  private final PantrySettingsRepository pantrySettingsRepository;
  private final OpenFoodFactsService openFoodFactsService;

  public PantryService(
      @Qualifier("pantryItemRepository") PantryItemRepository pantryItemRepository,
      PantryConsumptionEventRepository pantryConsumptionEventRepository,
      PantrySettingsRepository pantrySettingsRepository,
      OpenFoodFactsService openFoodFactsService
  ) {
    this.pantryItemRepository = pantryItemRepository;
    this.pantryConsumptionEventRepository = pantryConsumptionEventRepository;
    this.pantrySettingsRepository = pantrySettingsRepository;
    this.openFoodFactsService = openFoodFactsService;
  }

  public List<PantryItem> getAll() {
    // Only return active items for the pantry view
    return pantryItemRepository.findAllByCountGreaterThan(0);
  }

  public PantryItem addItem(PantryItemPostDTO postDTO) {
    if (postDTO == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body must not be empty");
    }

    Integer count = postDTO.getCount() == null ? 1 : postDTO.getCount();
    if (count <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count must be >= 1");
    }

    PantryItem item = new PantryItem();
    item.setCount(count);

    if (postDTO.getBarcode() != null && !postDTO.getBarcode().isBlank()) {
      ProductDTO p = openFoodFactsService.lookupByBarcode(postDTO.getBarcode().trim());
      item.setBarcode(p.getBarcode());
      item.setName(p.getName());
      item.setBrand(p.getBrand());
      item.setKcalPer100(p.getKcalPer100());
      item.setPackageQuantity(p.getPackageQuantity());
      item.setPackageUnit(p.getPackageUnit());
      item.setKcalPerPackage(p.getKcalPerPackage());
    }
    else {
      // manual item
      if (postDTO.getName() == null || postDTO.getName().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Either barcode must be provided or name must be set for manual items.");
      }
      item.setName(postDTO.getName().trim());
      item.setKcalPerPackage(postDTO.getKcalPerPackage());
    }

    PantryItem saved = pantryItemRepository.save(item);
    pantryItemRepository.flush();
    return saved;
  }

  public PantryItem consumeOne(Long id) {
    PantryItem item = pantryItemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry item not found"));

    int count = item.getCount() == null ? 1 : item.getCount();
    if (count <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is already consumed");
    }

    // Record the consumption event for statistics
    PantryConsumptionEvent ev = new PantryConsumptionEvent();
    ev.setPantryItemId(item.getId());
    ev.setBarcode(item.getBarcode());
    ev.setItemName(item.getName());
    ev.setKcal(item.getKcalPerPackage());
    pantryConsumptionEventRepository.save(ev);

    item.setLastConsumedAt(Instant.now());

    if (count == 1) {
      // Mark as fully consumed, but keep row for history/statistics.
      item.setCount(0);
      if (item.getConsumedAt() == null) {
        item.setConsumedAt(Instant.now());
      }
      PantryItem saved = pantryItemRepository.save(item);
      pantryItemRepository.flush();
      return saved;
    }

    item.setCount(count - 1);
    PantryItem saved = pantryItemRepository.save(item);
    pantryItemRepository.flush();
    return saved;
  }

  public void delete(Long id) {
    PantryItem item = pantryItemRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pantry item not found"));
    pantryItemRepository.delete(item);
    pantryItemRepository.flush();
  }

  /**
   * Global ideal daily kcal setting for the prototype.
   */
  public Double getIdealDailyKcal() {
    return pantrySettingsRepository.findById(1L).map(PantrySettings::getIdealDailyKcal).orElse(null);
  }

  public Double setIdealDailyKcal(Double idealDailyKcal) {
    if (idealDailyKcal == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idealDailyKcal must be provided");
    }
    if (idealDailyKcal <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idealDailyKcal must be > 0");
    }
    PantrySettings settings = pantrySettingsRepository.findById(1L).orElseGet(() -> {
      PantrySettings s = new PantrySettings();
      s.setId(1L);
      return s;
    });
    settings.setIdealDailyKcal(idealDailyKcal);
    pantrySettingsRepository.save(settings);
    pantrySettingsRepository.flush();
    return settings.getIdealDailyKcal();
  }

  /**
   * Computes the average daily calories consumed from a given date (inclusive).
   */
  public Stats computeStats(LocalDate fromDate) {
    if (fromDate == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date is required");
    }
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    long days = ChronoUnit.DAYS.between(fromDate, today) + 1;
    if (days < 1) days = 1;

    Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    List<PantryConsumptionEvent> events = pantryConsumptionEventRepository.findByConsumedAtGreaterThanEqual(fromInstant);

    double totalConsumed = 0.0;
    long unknown = 0;
    for (PantryConsumptionEvent ev : events) {
      if (ev.getKcal() == null) {
        unknown++;
      } else {
        totalConsumed += ev.getKcal();
      }
    }

    double avgDaily = totalConsumed / (double) days;

    // Total calories currently owned in pantry
    List<PantryItem> active = pantryItemRepository.findAllByCountGreaterThan(0);
    double totalOwned = 0.0;
    for (PantryItem it : active) {
      if (it.getKcalPerPackage() != null && it.getCount() != null) {
        totalOwned += it.getKcalPerPackage() * it.getCount();
      }
    }

    Double ideal = getIdealDailyKcal();
    Double delta = (ideal == null) ? null : (avgDaily - ideal);

    Stats s = new Stats();
    s.days = days;
    s.totalCaloriesOwned = totalOwned;
    s.totalCaloriesConsumed = totalConsumed;
    s.avgDailyCaloriesConsumed = avgDaily;
    s.idealDailyKcal = ideal;
    s.avgMinusIdeal = delta;
    s.unknownKcalEvents = unknown;
    return s;
  }

  /** Simple internal struct to avoid pulling extra libs. */
  public static class Stats {
    public long days;
    public double totalCaloriesOwned;
    public double totalCaloriesConsumed;
    public double avgDailyCaloriesConsumed;
    public Double idealDailyKcal;
    public Double avgMinusIdeal;
    public long unknownKcalEvents;
  }
}
