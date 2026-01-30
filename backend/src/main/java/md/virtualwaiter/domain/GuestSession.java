package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="guest_sessions")
public class GuestSession {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name="table_id", nullable=false)
  public Long tableId;

  @Column(nullable=false)
  public String locale = "ru";

  @Column(name="expires_at", nullable=false)
  public Instant expiresAt;

  @Column(name="session_secret")
  public String sessionSecret;

  @Column(name="created_by_ip")
  public String createdByIp;

  @Column(name="created_by_ua")
  public String createdByUa;

  @Column(name="party_id")
  public Long partyId;

  @Column(name="is_verified", nullable=false)
  public boolean isVerified = false;

  @Column(name="verified_phone")
  public String verifiedPhone;

  @Column(name="last_order_at")
  public Instant lastOrderAt;

  @Column(name="last_waiter_call_at")
  public Instant lastWaiterCallAt;

  @Column(name="last_bill_request_at")
  public Instant lastBillRequestAt;
}
