package md.virtualwaiter.service;

import md.virtualwaiter.domain.NotificationEvent;
import md.virtualwaiter.repo.NotificationEventRepo;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventService {
  private final NotificationEventRepo repo;

  public NotificationEventService(NotificationEventRepo repo) {
    this.repo = repo;
  }

  public void emit(long branchId, String type, long refId) {
    NotificationEvent ev = new NotificationEvent();
    ev.branchId = branchId;
    ev.eventType = type;
    ev.refId = refId;
    repo.save(ev);
  }
}
