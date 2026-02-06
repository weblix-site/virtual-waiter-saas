package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "menu_tags")
public class MenuTag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long tenantId;

  @Column(nullable = false, length = 100)
  public String name;

  @Column(nullable = false, length = 100)
  public String slug;

  @Column(nullable = false)
  public boolean isAllergen;

  @Column(nullable = false)
  public boolean isActive;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
