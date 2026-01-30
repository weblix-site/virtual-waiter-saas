package md.virtualwaiter.service;

import md.virtualwaiter.domain.AuditLog;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.repo.AuditLogRepo;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepo repo;

  public AuditService(AuditLogRepo repo) {
    this.repo = repo;
  }

  public void log(StaffUser actor, String action, String entityType, Long entityId, String detailsJson) {
    AuditLog a = new AuditLog();
    if (actor != null) {
      a.actorUserId = actor.id;
      a.actorUsername = actor.username;
      a.actorRole = actor.role;
      a.branchId = actor.branchId;
    }
    a.action = action;
    a.entityType = entityType;
    a.entityId = entityId;
    a.detailsJson = detailsJson;
    repo.save(a);
  }
}
