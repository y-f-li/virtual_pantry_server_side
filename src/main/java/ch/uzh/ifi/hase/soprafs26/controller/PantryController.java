package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryBudgetPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryStatsDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GuestSessionService;
import ch.uzh.ifi.hase.soprafs26.service.PantryService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class PantryController {

  private final PantryService pantryService;
  private final UserService userService;
  private final GuestSessionService guestSessionService;

  public PantryController(PantryService pantryService, UserService userService, GuestSessionService guestSessionService) {
    this.pantryService = pantryService;
    this.userService = userService;
    this.guestSessionService = guestSessionService;
  }

  @GetMapping("/pantry")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<PantryItemGetDTO> getPantry(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);

    List<PantryItem> items = isGuestToken(token)
        ? guestSessionService.getAll(token)
        : getAuthenticatedPantryItems(token);
    List<PantryItemGetDTO> dtos = new ArrayList<>();
    for (PantryItem item : items) {
      dtos.add(DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(item));
    }
    return dtos;
  }

  @PostMapping("/pantry")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public PantryItemGetDTO addToPantry(
      @RequestBody PantryItemPostDTO pantryItemPostDTO,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);

    PantryItem created = isGuestToken(token)
        ? guestSessionService.addItem(token, pantryItemPostDTO)
        : addAuthenticatedPantryItem(token, pantryItemPostDTO);
    return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(created);
  }

  @PatchMapping("/pantry/{id}/consume")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public PantryItemGetDTO consumeOne(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);

    PantryItem updatedOrDeleted = isGuestToken(token)
        ? guestSessionService.consumeOne(token, id)
        : consumeAuthenticatedItem(token, id);
    return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(updatedOrDeleted);
  }

  @DeleteMapping("/pantry/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);
    if (isGuestToken(token)) {
      guestSessionService.delete(token, id);
      return;
    }
    userService.authenticate(token);
    pantryService.delete(id);
  }

  @GetMapping("/pantry/stats")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public PantryStatsDTO stats(
      @RequestParam("from") String from,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);

    LocalDate fromDate = LocalDate.parse(from);
    PantryService.Stats stats = isGuestToken(token)
        ? guestSessionService.computeStats(token, fromDate)
        : computeAuthenticatedStats(token, fromDate);

    PantryStatsDTO dto = new PantryStatsDTO();
    dto.setFromDate(fromDate.toString());
    dto.setDays(stats.days);
    dto.setTotalCaloriesOwned(stats.totalCaloriesOwned);
    dto.setTotalCaloriesConsumed(stats.totalCaloriesConsumed);
    dto.setAvgDailyCaloriesConsumed(stats.avgDailyCaloriesConsumed);
    dto.setIdealDailyKcal(stats.idealDailyKcal);
    dto.setAvgMinusIdeal(stats.avgMinusIdeal);
    dto.setUnknownKcalEvents(stats.unknownKcalEvents);
    return dto;
  }

  @PutMapping("/pantry/budget")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public PantryBudgetPutDTO setBudget(
      @RequestBody PantryBudgetPutDTO body,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);

    Double saved = isGuestToken(token)
        ? guestSessionService.setIdealDailyKcal(token, body.getIdealDailyKcal())
        : setAuthenticatedBudget(token, body.getIdealDailyKcal());
    PantryBudgetPutDTO out = new PantryBudgetPutDTO();
    out.setIdealDailyKcal(saved);
    return out;
  }


  private boolean isGuestToken(String token) {
    return guestSessionService.isGuestToken(token);
  }

  private List<PantryItem> getAuthenticatedPantryItems(String token) {
    userService.authenticate(token);
    return pantryService.getAll();
  }

  private PantryItem addAuthenticatedPantryItem(String token, PantryItemPostDTO pantryItemPostDTO) {
    userService.authenticate(token);
    return pantryService.addItem(pantryItemPostDTO);
  }

  private PantryItem consumeAuthenticatedItem(String token, Long id) {
    userService.authenticate(token);
    return pantryService.consumeOne(id);
  }

  private PantryService.Stats computeAuthenticatedStats(String token, LocalDate fromDate) {
    userService.authenticate(token);
    return pantryService.computeStats(fromDate);
  }

  private Double setAuthenticatedBudget(String token, Double idealDailyKcal) {
    userService.authenticate(token);
    return pantryService.setIdealDailyKcal(idealDailyKcal);
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) return null;
    if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
    return authHeader;
  }
}
