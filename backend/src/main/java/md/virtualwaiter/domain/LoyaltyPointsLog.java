package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "loyalty_points_log")
public class LoyaltyPointsLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String phone;

  @Column(name = "bill_request_id")
  public Long billRequestId;

  @Column(name = "delta_points", nullable = false)
  public int deltaPoints;

  @Column
  public String reason;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
