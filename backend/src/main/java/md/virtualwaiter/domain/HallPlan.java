package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "hall_plans")
public class HallPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "hall_id", nullable = false)
  public Long hallId;

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
}
