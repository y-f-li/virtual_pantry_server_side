package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "pantry_items")
public class PantryItem implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column
  private String barcode;

  @Column
  private String brand;

  @Column(nullable = false)
  private Integer count;

  @Column
  private Double kcalPer100;

  @Column
  private Double packageQuantity;

  @Column
  private String packageUnit;

  @Column
  private Double kcalPerPackage;

  @Column(nullable = false)
  private Instant createdAt;

  /**
   * The timestamp when this pantry item was fully consumed (i.e., when count first reached 0).
   * We keep the row for historical/statistics purposes but exclude count==0 items from /pantry.
   */
  @Column
  private Instant consumedAt;

  /**
   * The timestamp of the latest "consume 1" action for this item.
   */
  @Column
  private Instant lastConsumedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
    if (count == null) count = 1;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getBarcode() { return barcode; }
  public void setBarcode(String barcode) { this.barcode = barcode; }

  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }

  public Integer getCount() { return count; }
  public void setCount(Integer count) { this.count = count; }

  public Double getKcalPer100() { return kcalPer100; }
  public void setKcalPer100(Double kcalPer100) { this.kcalPer100 = kcalPer100; }

  public Double getPackageQuantity() { return packageQuantity; }
  public void setPackageQuantity(Double packageQuantity) { this.packageQuantity = packageQuantity; }

  public String getPackageUnit() { return packageUnit; }
  public void setPackageUnit(String packageUnit) { this.packageUnit = packageUnit; }

  public Double getKcalPerPackage() { return kcalPerPackage; }
  public void setKcalPerPackage(Double kcalPerPackage) { this.kcalPerPackage = kcalPerPackage; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getConsumedAt() { return consumedAt; }
  public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

  public Instant getLastConsumedAt() { return lastConsumedAt; }
  public void setLastConsumedAt(Instant lastConsumedAt) { this.lastConsumedAt = lastConsumedAt; }
}