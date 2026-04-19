package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class PantryItemPostDTO {
  private String barcode;
  private String name;
  private Double kcalPerPackage;
  private Integer count;

  public String getBarcode() { return barcode; }
  public void setBarcode(String barcode) { this.barcode = barcode; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public Double getKcalPerPackage() { return kcalPerPackage; }
  public void setKcalPerPackage(Double kcalPerPackage) { this.kcalPerPackage = kcalPerPackage; }

  public Integer getCount() { return count; }
  public void setCount(Integer count) { this.count = count; }
}