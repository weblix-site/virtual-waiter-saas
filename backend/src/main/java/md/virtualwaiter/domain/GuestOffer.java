package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_offers")
public class GuestOffer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String phone;

  @Column(nullable = false)
  public String title;

  @Column
  public String body;

  @Column(name = "discount_code")
  public String discountCode;

  @Column(name = "starts_at")
  public Instant startsAt;

  @Column(name = "ends_at")
  public Instant endsAt;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  public Instant updatedAt;
}
