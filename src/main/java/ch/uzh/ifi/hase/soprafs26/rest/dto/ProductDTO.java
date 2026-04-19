package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class ProductDTO {
  private String barcode;
  private String name;
  private String brand;

  private Double kcalPer100;

  private Double packageQuantity;
  private String packageUnit;

  private Double kcalPerPackage;

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
}