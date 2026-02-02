package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "branch_halls")
public class BranchHall {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String name;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "sort_order", nullable = false)
  public int sortOrder = 0;

  @Column(name = "layout_bg_url")
  public String layoutBgUrl;

  @Column(name = "layout_zones_json")
  public String layoutZonesJson;

  @Column(name = "active_plan_id")
  public Long activePlanId;
}
