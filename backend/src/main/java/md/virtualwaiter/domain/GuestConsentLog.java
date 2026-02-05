package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_consent_logs")
public class GuestConsentLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  @Column(name = "phone_e164", nullable = false)
  public String phoneE164;

  @Column(name = "branch_id")
  public Long branchId;

  @Column(name = "consent_type", nullable = false)
  public String consentType;

  @Column(name = "accepted", nullable = false)
  public boolean accepted;

  @Column(name = "text_version", nullable = false)
  public String textVersion;

  @Column(name = "ip")
  public String ip;

  @Column(name = "user_agent")
  public String userAgent;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
