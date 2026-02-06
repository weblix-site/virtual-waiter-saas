package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "branch_recommendation_template_items")
public class BranchRecommendationTemplateItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "template_id", nullable = false)
  public Long templateId;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "sort_order", nullable = false)
  public int sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
