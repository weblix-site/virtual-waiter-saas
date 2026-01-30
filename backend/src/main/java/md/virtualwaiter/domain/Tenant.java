package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "tenants")
public class Tenant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public String name;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
