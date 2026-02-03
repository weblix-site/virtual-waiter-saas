package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "intent_id", nullable = false)
  public Long intentId;

  @Column(nullable = false)
  public String provider;

  /** CREATED | AUTHORIZED | CAPTURED | FAILED | CANCELLED */
  @Column(nullable = false)
  public String status;

  @Column(name = "amount_cents", nullable = false)
  public int amountCents;

  @Column(name = "provider_ref")
  public String providerRef;

  @Column(name = "provider_payload")
  public String providerPayload;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
