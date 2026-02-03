package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "table_id", nullable = false)
  public Long tableId;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  @Column(name = "bill_request_id")
  public Long billRequestId;

  @Column(nullable = false)
  public String provider;

  /** CREATED | PENDING | PAID | FAILED | CANCELLED */
  @Column(nullable = false)
  public String status = "CREATED";

  @Column(name = "amount_cents", nullable = false)
  public int amountCents;

  @Column(name = "currency_code", nullable = false)
  public String currencyCode;

  @Column(name = "items_json")
  public String itemsJson;

  @Column(name = "provider_ref")
  public String providerRef;

  @Column(name = "return_url")
  public String returnUrl;

  @Column(name = "cancel_url")
  public String cancelUrl;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  public Instant updatedAt;
}
