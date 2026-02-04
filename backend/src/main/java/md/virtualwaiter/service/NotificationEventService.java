package md.virtualwaiter.service;

import md.virtualwaiter.domain.NotificationEvent;
import md.virtualwaiter.repo.NotificationEventRepo;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import md.virtualwaiter.util.PayloadGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationEventService {
  private static final Logger log = LoggerFactory.getLogger(NotificationEventService.class);
  private final NotificationEventRepo repo;
  private final StaffPushService pushService;
  private final int maxEventPayloadBytes;

  public NotificationEventService(
    NotificationEventRepo repo,
    StaffPushService pushService,
    @Value("${app.payload.maxBytes:4096}") int maxEventPayloadBytes
  ) {
    this.repo = repo;
    this.pushService = pushService;
    this.maxEventPayloadBytes = maxEventPayloadBytes;
  }

  public void emit(long branchId, String type, long refId) {
    String typeSafe = PayloadGuard.truncateBytes(type, maxEventPayloadBytes);
    if (typeSafe != null && !typeSafe.equals(type)) {
      log.warn("Notification event type truncated to {} bytes", maxEventPayloadBytes);
    }
    NotificationEvent ev = new NotificationEvent();
    ev.branchId = branchId;
    ev.eventType = typeSafe;
    ev.refId = refId;
    repo.save(ev);
    pushService.notifyBranch(branchId, typeSafe, refId);
  }
}
