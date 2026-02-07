package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "branch_feature_flags")
public class BranchFeatureFlag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "flag_id", nullable = false)
  public Long flagId;

  @Column(name = "enabled", nullable = false)
  public boolean enabled;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt = Instant.now();
}
