package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "menu_time_slots")
public class MenuTimeSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String name;

  @Column(name = "days_mask", nullable = false)
  public int daysMask = 127;

  @Column(name = "start_time", nullable = false)
  public LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  public LocalTime endTime;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt = Instant.now();
}
