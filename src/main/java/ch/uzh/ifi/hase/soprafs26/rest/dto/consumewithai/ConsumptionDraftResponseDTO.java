package ch.uzh.ifi.hase.soprafs26.rest.dto.consumewithai;

import java.util.List;

public class ConsumptionDraftResponseDTO {

    private List<ConsumptionDraftItemDTO> items;

    public List<ConsumptionDraftItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ConsumptionDraftItemDTO> items) {
        this.items = items;
    }
}
