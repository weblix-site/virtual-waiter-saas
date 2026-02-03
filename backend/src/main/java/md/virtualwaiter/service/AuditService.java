package md.virtualwaiter.service;

import md.virtualwaiter.domain.AuditLog;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.repo.AuditLogRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import md.virtualwaiter.util.PayloadGuard;

@Service
public class AuditService {
  private static final Logger log = LoggerFactory.getLogger(AuditService.class);
  private final AuditLogRepo repo;
  private final int maxDetailsChars;
  private final int maxLogPayloadChars;

  public AuditService(
    AuditLogRepo repo,
    @Value("${app.audit.maxDetailsChars:4000}") int maxDetailsChars,
    @Value("${app.log.maxPayloadChars:2000}") int maxLogPayloadChars
  ) {
    this.repo = repo;
    this.maxDetailsChars = maxDetailsChars;
    this.maxLogPayloadChars = maxLogPayloadChars;
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
    a.detailsJson = truncate(detailsJson);
    repo.save(a);
  }

  private String truncate(String detailsJson) {
    if (detailsJson == null) return null;
    if (maxDetailsChars <= 0) return null;
    if (detailsJson.length() <= maxDetailsChars) return detailsJson;
    log.warn("Audit detailsJson truncated (maxDetailsChars={}, preview={})", maxDetailsChars, PayloadGuard.truncate(detailsJson, maxLogPayloadChars));
    String suffix = "...(truncated)";
    if (maxDetailsChars <= suffix.length()) {
      return detailsJson.substring(0, maxDetailsChars);
    }
    return detailsJson.substring(0, maxDetailsChars - suffix.length()) + suffix;
  }
}
