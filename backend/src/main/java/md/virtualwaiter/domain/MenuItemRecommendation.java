package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "menu_item_recommendations")
public class MenuItemRecommendation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long tenantId;

  @Column(nullable = false)
  public Long sourceItemId;

  @Column(nullable = false)
  public Long targetItemId;

  @Column(nullable = false, length = 32)
  public String type;

  @Column(nullable = false)
  public int sortOrder;

  @Column(nullable = false)
  public boolean isActive = true;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
