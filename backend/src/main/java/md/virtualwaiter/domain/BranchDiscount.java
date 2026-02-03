package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "branch_discounts")
public class BranchDiscount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  /** COUPON | HAPPY_HOUR */
  @Column(nullable = false)
  public String scope = "COUPON";

  @Column
  public String code;

  /** PERCENT | FIXED */
  @Column(nullable = false)
  public String type;

  /** percent (1..100) or fixed cents */
  @Column(nullable = false)
  public int value;

  @Column
  public String label;

  @Column(nullable = false)
  public boolean active = true;

  @Column(name = "max_uses")
  public Integer maxUses;

  @Column(name = "used_count", nullable = false)
  public int usedCount = 0;

  @Column(name = "starts_at")
  public Instant startsAt;

  @Column(name = "ends_at")
  public Instant endsAt;

  /** Bitmask, Monday=1<<0 .. Sunday=1<<6 */
  @Column(name = "days_mask")
  public Integer daysMask;

  @Column(name = "start_minute")
  public Integer startMinute;

  @Column(name = "end_minute")
  public Integer endMinute;

  @Column(name = "tz_offset_minutes")
  public Integer tzOffsetMinutes;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
