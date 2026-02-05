package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  @Column(name = "phone_e164", nullable = false)
  public String phoneE164;

  @Column(name = "channel", nullable = false)
  public String channel; // SMS / WHATSAPP / TELEGRAM

  @Column(name = "otp_hash", nullable = false)
  public String otpHash;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  @Column(name = "attempts_left", nullable = false)
  public int attemptsLeft;

  @Column(name = "status", nullable = false)
  public String status; // SENT / VERIFIED / EXPIRED / LOCKED

  @Column(name = "delivery_status", nullable = false)
  public String deliveryStatus; // SENT / QUEUED / FAILED

  @Column(name = "delivery_provider_ref")
  public String deliveryProviderRef;

  @Column(name = "delivery_error")
  public String deliveryError;

  @Column(name = "delivered_at")
  public Instant deliveredAt;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
