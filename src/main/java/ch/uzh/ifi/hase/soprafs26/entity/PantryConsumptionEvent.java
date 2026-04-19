package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Records a single "consume 1" action. This gives us reliable statistics
 * even if pantry items are later edited or removed.
 */
@Entity
@Table(name = "pantry_consumption_events")
public class PantryConsumptionEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false)
  private Long pantryItemId;

  @Column
  private String barcode;

  @Column(nullable = false)
  private String itemName;

  /** kcal for the consumed unit/package (may be null if unknown). */
  @Column
  private Double kcal;

  @Column(nullable = false)
  private Instant consumedAt;

  @PrePersist
  protected void onCreate() {
    if (consumedAt == null) consumedAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getPantryItemId() { return pantryItemId; }
  public void setPantryItemId(Long pantryItemId) { this.pantryItemId = pantryItemId; }

  public String getBarcode() { return barcode; }
  public void setBarcode(String barcode) { this.barcode = barcode; }

  public String getItemName() { return itemName; }
  public void setItemName(String itemName) { this.itemName = itemName; }

  public Double getKcal() { return kcal; }
  public void setKcal(Double kcal) { this.kcal = kcal; }

  public Instant getConsumedAt() { return consumedAt; }
  public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
}
