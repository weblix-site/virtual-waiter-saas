package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "modifier_groups")
public class ModifierGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "name_ru", nullable = false)
  public String nameRu;

  @Column(name = "name_ro")
  public String nameRo;

  @Column(name = "name_en")
  public String nameEn;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
