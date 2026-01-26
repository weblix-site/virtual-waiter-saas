package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_items")
public class MenuItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "category_id", nullable = false)
  public Long categoryId;

  @Column(name = "name_ru", nullable = false)
  public String nameRu;

  @Column(name = "name_ro")
  public String nameRo;

  @Column(name = "name_en")
  public String nameEn;

  @Column(name = "description_ru")
  public String descriptionRu;

  @Column(name = "description_ro")
  public String descriptionRo;

  @Column(name = "description_en")
  public String descriptionEn;

  // characteristics
  @Column(name = "ingredients_ru")
  public String ingredientsRu;

  @Column(name = "ingredients_ro")
  public String ingredientsRo;

  @Column(name = "ingredients_en")
  public String ingredientsEn;

  /** Free text or comma-separated list (MVP) */
  @Column(name = "allergens")
  public String allergens;

  /** e.g. "350 g" / "0.5 l" */
  @Column(name = "weight")
  public String weight;

  /** Tags as comma-separated string (MVP) */
  @Column(name = "tags")
  public String tags;

  /** Photo URLs as comma-separated string (MVP) */
  @Column(name = "photo_urls")
  public String photoUrls;

  @Column(name = "price_cents", nullable = false)
  public int priceCents;

  @Column(nullable = false)
  public String currency = "MDL";

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
