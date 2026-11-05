package ch.uzh.ifi.hase.soprafs26.rest.dto.consumewithai;

public class ConsumptionDraftItemDTO {

    private Long pantryItemId;
    private String sourcePhrase;
    private Double amount;
    private String amountUnit;

    public Long getPantryItemId() {
        return pantryItemId;
    }

    public void setPantryItemId(Long pantryItemId) {
        this.pantryItemId = pantryItemId;
    }

    public String getSourcePhrase() {
        return sourcePhrase;
    }

    public void setSourcePhrase(String sourcePhrase) {
        this.sourcePhrase = sourcePhrase;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getAmountUnit() {
        return amountUnit;
    }

    public void setAmountUnit(String amountUnit) {
        this.amountUnit = amountUnit;
    }
}
