package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Simple global settings for the pantry prototype.
 *
 * If/when you introduce households, this can become HouseholdSettings
 * keyed by householdId.
 */
@Entity
@Table(name = "pantry_settings")
public class PantrySettings implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  @Column
  private Double idealDailyKcal;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Double getIdealDailyKcal() { return idealDailyKcal; }
  public void setIdealDailyKcal(Double idealDailyKcal) { this.idealDailyKcal = idealDailyKcal; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
