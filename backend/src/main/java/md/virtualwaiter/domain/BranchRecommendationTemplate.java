package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "branch_recommendation_templates")
public class BranchRecommendationTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "tenant_id", nullable = false)
  public Long tenantId;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(name = "sort_order", nullable = false)
  public int sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
