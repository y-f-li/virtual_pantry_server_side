package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryBudgetPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryStatsDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
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

  public PantryController(PantryService pantryService, UserService userService) {
    this.pantryService = pantryService;
    this.userService = userService;
  }

  @GetMapping("/pantry")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<PantryItemGetDTO> getPantry(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    userService.authenticate(extractToken(authHeader));

    List<PantryItem> items = pantryService.getAll();
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
    userService.authenticate(extractToken(authHeader));

    PantryItem created = pantryService.addItem(pantryItemPostDTO);
    return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(created);
  }

  @PatchMapping("/pantry/{id}/consume")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public PantryItemGetDTO consumeOne(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    userService.authenticate(extractToken(authHeader));

    PantryItem updatedOrDeleted = pantryService.consumeOne(id);
    return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(updatedOrDeleted);
  }

  @DeleteMapping("/pantry/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable("id") Long id,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    userService.authenticate(extractToken(authHeader));
    pantryService.delete(id);
  }

  @GetMapping("/pantry/stats")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public PantryStatsDTO stats(
      @RequestParam("from") String from,
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    userService.authenticate(extractToken(authHeader));

    LocalDate fromDate = LocalDate.parse(from);
    PantryService.Stats stats = pantryService.computeStats(fromDate);

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
    userService.authenticate(extractToken(authHeader));

    Double saved = pantryService.setIdealDailyKcal(body.getIdealDailyKcal());
    PantryBudgetPutDTO out = new PantryBudgetPutDTO();
    out.setIdealDailyKcal(saved);
    return out;
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) return null;
    if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
    return authHeader;
  }
}
