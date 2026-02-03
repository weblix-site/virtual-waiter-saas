package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "actor_user_id")
  public Long actorUserId;

  @Column(name = "actor_username")
  public String actorUsername;

  @Column(name = "actor_role")
  public String actorRole;

  @Column(name = "branch_id")
  public Long branchId;

  @Column(name = "action", nullable = false)
  public String action;

  @Column(name = "entity_type", nullable = false)
  public String entityType;

  @Column(name = "entity_id")
  public Long entityId;

  @Column(name = "details_json")
  public String detailsJson;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
