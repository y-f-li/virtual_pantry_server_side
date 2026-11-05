package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ConsumePantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ConsumePantryItemResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryBulkAddPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryOverviewGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.PantryService;

import ch.uzh.ifi.hase.soprafs26.rest.dto.consumewithai.ConsumptionDraftPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.consumewithai.ConsumptionDraftResponseDTO;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import ch.uzh.ifi.hase.soprafs26.service.ConsumeWithAIService;

@RestController
public class PantryController {

    private final PantryService pantryService;
    private final ConsumeWithAIService consumeWithAIService;

    public PantryController(PantryService pantryService, ConsumeWithAIService consumeWithAIService) {
        this.pantryService = pantryService;
        this.consumeWithAIService = consumeWithAIService;
    }

    @PostMapping("/households/{householdId}/pantry")
    @ResponseStatus(HttpStatus.CREATED)
    public PantryItemGetDTO addPantryItem(
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            @PathVariable Long householdId,
            @RequestBody PantryItemPostDTO pantryItemPostDTO) {

        PantryItem pantryItem = pantryService.addItem(
                householdId,
                pantryItemPostDTO,
                authenticatedUserId
        );

        return DTOMapper.INSTANCE.convertEntityToPantryItemGetDTO(pantryItem);
    }

    @PostMapping("/households/{householdId}/pantry/bulk-add")
    @ResponseStatus(HttpStatus.OK)
    public List<PantryItemGetDTO> bulkAddPantryItems(
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            @PathVariable Long householdId,
            @RequestBody PantryBulkAddPostDTO bulkBody) {

        List<PantryItem> saved = pantryService.bulkAddItems(
                householdId,
                bulkBody != null ? bulkBody.getItems() : null,
                authenticatedUserId
        );

        return saved.stream()
                .map(DTOMapper.INSTANCE::convertEntityToPantryItemGetDTO)
                .toList();
    }

    @PostMapping("/households/{householdId}/pantry/{itemId}/consume")
    @ResponseStatus(HttpStatus.OK)
    public ConsumePantryItemResponseDTO consumePantryItem(
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            @PathVariable Long householdId,
            @PathVariable Long itemId,
            @RequestBody ConsumePantryItemPostDTO consumePostDTO) {

        PantryService.ConsumeResult result = pantryService.consumeItem(
                householdId,
                itemId,
                consumePostDTO.getAmount(),
                consumePostDTO.getAmountUnit(),
                consumePostDTO.getKcalPerPackage(),
                Boolean.TRUE.equals(consumePostDTO.getSkipCalorieLogging()),
                authenticatedUserId,
                consumePostDTO.getConsumedForUserId()
        );

        ConsumePantryItemResponseDTO responseDTO = new ConsumePantryItemResponseDTO();
        responseDTO.setItemId(result.getItemId());
        responseDTO.setRemainingAmount(result.getRemainingAmount());
        responseDTO.setConsumedCalories(result.getConsumedCalories());
        responseDTO.setRemoved(result.isRemoved());

        return responseDTO;
    }

    @PostMapping("/households/{householdId}/pantry/{itemId}/remove")
    @ResponseStatus(HttpStatus.OK)
    public ConsumePantryItemResponseDTO removePantryItem(
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            @PathVariable Long householdId,
            @PathVariable Long itemId,
            @RequestBody ConsumePantryItemPostDTO removePostDTO) {

        PantryService.ConsumeResult result = pantryService.removeItem(
                householdId,
                itemId,
                removePostDTO.getAmount(),
                authenticatedUserId
        );

        ConsumePantryItemResponseDTO responseDTO = new ConsumePantryItemResponseDTO();
        responseDTO.setItemId(result.getItemId());
        responseDTO.setRemainingAmount(result.getRemainingAmount());
        responseDTO.setConsumedCalories(result.getConsumedCalories());
        responseDTO.setRemoved(result.isRemoved());

        return responseDTO;
    }

    @GetMapping("/households/{householdId}/pantry")
    @ResponseStatus(HttpStatus.OK)
    public PantryOverviewGetDTO getPantry(
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            @PathVariable Long householdId) {

        List<PantryItem> pantryItems = pantryService.getPantryItems(
                householdId,
                authenticatedUserId
        );

        double totalCalories = pantryService.calculateTotalCalories(householdId);

        List<PantryItemGetDTO> itemDTOs = pantryItems.stream()
                .map(DTOMapper.INSTANCE::convertEntityToPantryItemGetDTO)
                .toList();

        PantryOverviewGetDTO responseDTO = new PantryOverviewGetDTO();
        responseDTO.setItems(itemDTOs);
        responseDTO.setTotalCalories(totalCalories);

        return responseDTO;
    }


        @PostMapping("/households/{householdId}/pantry/consume-with-ai")
        public ConsumptionDraftResponseDTO createConsumptionDraft(
                @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
                @PathVariable Long householdId,
                @RequestBody ConsumptionDraftPostDTO requestDTO
        ) throws JsonProcessingException {

                if (requestDTO.getDescription() == null
                        || requestDTO.getDescription().isBlank()) {
                        throw new IllegalArgumentException(
                        "Prompt must not be empty."
                        );
                }

                List<PantryItem> pantryItems = pantryService.getPantryItems(
                        householdId,
                        authenticatedUserId
                );

                return consumeWithAIService.generateDraft(
                        requestDTO.getDescription(),
                        pantryItems
                );
        }

}
