package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;

public class PantryItemGetDTO {
  private Long id;
  private String barcode;
  private String name;
  private String brand;

  private Double kcalPer100;
  private Double packageQuantity;
  private String packageUnit;
  private Double kcalPerPackage;

  private Integer count;
  private Instant createdAt;

  private Instant consumedAt;
  private Instant lastConsumedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getBarcode() { return barcode; }
  public void setBarcode(String barcode) { this.barcode = barcode; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }

  public Double getKcalPer100() { return kcalPer100; }
  public void setKcalPer100(Double kcalPer100) { this.kcalPer100 = kcalPer100; }

  public Double getPackageQuantity() { return packageQuantity; }
  public void setPackageQuantity(Double packageQuantity) { this.packageQuantity = packageQuantity; }

  public String getPackageUnit() { return packageUnit; }
  public void setPackageUnit(String packageUnit) { this.packageUnit = packageUnit; }

  public Double getKcalPerPackage() { return kcalPerPackage; }
  public void setKcalPerPackage(Double kcalPerPackage) { this.kcalPerPackage = kcalPerPackage; }

  public Integer getCount() { return count; }
  public void setCount(Integer count) { this.count = count; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getConsumedAt() { return consumedAt; }
  public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

  public Instant getLastConsumedAt() { return lastConsumedAt; }
  public void setLastConsumedAt(Instant lastConsumedAt) { this.lastConsumedAt = lastConsumedAt; }
}