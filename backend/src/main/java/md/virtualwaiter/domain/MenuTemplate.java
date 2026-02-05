package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "menu_templates")
public class MenuTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "tenant_id", nullable = false)
  public Long tenantId;

  @Column(name = "restaurant_id")
  public Long restaurantId;

  @Column(nullable = false)
  public String name;

  @Column(name = "payload_json", nullable = false)
  public String payloadJson;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt = Instant.now();
}
