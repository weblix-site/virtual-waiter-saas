package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  /** Optional video URL (MVP) */
  @Column(name = "video_url")
  public String videoUrl;

  @Column(name = "kcal")
  public Integer kcal;

  @Column(name = "protein_g")
  public Integer proteinG;

  @Column(name = "fat_g")
  public Integer fatG;

  @Column(name = "carbs_g")
  public Integer carbsG;

  @Column(name = "price_cents", nullable = false)
  public int priceCents;

  @Column(nullable = false)
  public String currency = "MDL";

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "is_stop_list", nullable = false)
  public boolean isStopList = false;
}
