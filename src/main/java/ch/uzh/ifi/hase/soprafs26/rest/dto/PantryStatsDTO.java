package ch.uzh.ifi.hase.soprafs26.rest.dto;

/** Response for /pantry/stats */
public class PantryStatsDTO {
  private String fromDate; // yyyy-mm-dd
  private long days;

  private double totalCaloriesOwned;
  private double totalCaloriesConsumed;
  private double avgDailyCaloriesConsumed;

  private Double idealDailyKcal;
  private Double avgMinusIdeal;

  private long unknownKcalEvents;

  public String getFromDate() { return fromDate; }
  public void setFromDate(String fromDate) { this.fromDate = fromDate; }

  public long getDays() { return days; }
  public void setDays(long days) { this.days = days; }

  public double getTotalCaloriesOwned() { return totalCaloriesOwned; }
  public void setTotalCaloriesOwned(double totalCaloriesOwned) { this.totalCaloriesOwned = totalCaloriesOwned; }

  public double getTotalCaloriesConsumed() { return totalCaloriesConsumed; }
  public void setTotalCaloriesConsumed(double totalCaloriesConsumed) { this.totalCaloriesConsumed = totalCaloriesConsumed; }

  public double getAvgDailyCaloriesConsumed() { return avgDailyCaloriesConsumed; }
  public void setAvgDailyCaloriesConsumed(double avgDailyCaloriesConsumed) { this.avgDailyCaloriesConsumed = avgDailyCaloriesConsumed; }

  public Double getIdealDailyKcal() { return idealDailyKcal; }
  public void setIdealDailyKcal(Double idealDailyKcal) { this.idealDailyKcal = idealDailyKcal; }

  public Double getAvgMinusIdeal() { return avgMinusIdeal; }
  public void setAvgMinusIdeal(Double avgMinusIdeal) { this.avgMinusIdeal = avgMinusIdeal; }

  public long getUnknownKcalEvents() { return unknownKcalEvents; }
  public void setUnknownKcalEvents(long unknownKcalEvents) { this.unknownKcalEvents = unknownKcalEvents; }
}
