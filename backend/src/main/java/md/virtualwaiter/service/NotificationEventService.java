package md.virtualwaiter.service;

import md.virtualwaiter.domain.NotificationEvent;
import md.virtualwaiter.repo.NotificationEventRepo;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventService {
  private final NotificationEventRepo repo;
  private final StaffPushService pushService;

  public NotificationEventService(NotificationEventRepo repo, StaffPushService pushService) {
    this.repo = repo;
    this.pushService = pushService;
  }

  public void emit(long branchId, String type, long refId) {
    NotificationEvent ev = new NotificationEvent();
    ev.branchId = branchId;
    ev.eventType = type;
    ev.refId = refId;
    repo.save(ev);
    pushService.notifyBranch(branchId, type, refId);
  }
}
