package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "bill_requests")
public class BillRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "table_id", nullable = false)
  public Long tableId;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  @Column(name = "party_id")
  public Long partyId;

  /** MY | SELECTED | WHOLE_TABLE */
  @Column(nullable = false)
  public String mode;

  /** CASH | TERMINAL */
  @Column(name = "payment_method", nullable = false)
  public String paymentMethod;

  /** CREATED | PAID_CONFIRMED | CANCELLED | CLOSED | EXPIRED */
  @Column(nullable = false)
  public String status = "CREATED";

  @Column(name = "subtotal_cents", nullable = false)
  public int subtotalCents;

  @Column(name = "tips_percent")
  public Integer tipsPercent;

  @Column(name = "tips_amount_cents", nullable = false)
  public int tipsAmountCents;

  @Column(name = "total_cents", nullable = false)
  public int totalCents;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "created_by_ip")
  public String createdByIp;

  @Column(name = "created_by_ua")
  public String createdByUa;

  @Column(name = "confirmed_at")
  public Instant confirmedAt;

  @Column(name = "confirmed_by_staff_id")
  public Long confirmedByStaffId;
}
