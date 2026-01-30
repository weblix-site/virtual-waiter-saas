package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "branches")
public class Branch {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "tenant_id", nullable = false)
  public Long tenantId;

  @Column(nullable = false)
  public String name;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
