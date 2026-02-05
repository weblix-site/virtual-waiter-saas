package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "staff_device_tokens")
public class StaffDeviceToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "staff_user_id", nullable = false)
  public Long staffUserId;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String platform;

  @Column(name = "device_id")
  public String deviceId;

  @Column(name = "device_name")
  public String deviceName;

  @Column(nullable = false, unique = true)
  public String token;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "last_seen_at", nullable = false)
  public Instant lastSeenAt = Instant.now();

  @Column(name = "revoked_at")
  public Instant revokedAt;
}
