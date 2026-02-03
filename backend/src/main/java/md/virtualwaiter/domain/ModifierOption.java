package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "modifier_options")
public class ModifierOption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "group_id", nullable = false)
  public Long groupId;

  @Column(name = "name_ru", nullable = false)
  public String nameRu;

  @Column(name = "name_ro")
  public String nameRo;

  @Column(name = "name_en")
  public String nameEn;

  @Column(name = "price_cents", nullable = false)
  public int priceCents = 0;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
