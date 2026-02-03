package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "hall_plan_versions")
public class HallPlanVersion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long planId;

  @Column(nullable = false)
  public Long hallId;

  @Column(nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String name;

  @Column(nullable = false)
  public int sortOrder;

  @Column(nullable = false)
  public boolean isActive = true;

  @Column
  public String layoutBgUrl;

  @Column(columnDefinition = "text")
  public String layoutZonesJson;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  @Column
  public Long createdByStaffId;

  @Column
  public String action;
}
