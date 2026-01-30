package md.virtualwaiter.repo;

import md.virtualwaiter.domain.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationEventRepo extends JpaRepository<NotificationEvent, Long> {
  List<NotificationEvent> findTop200ByBranchIdAndIdGreaterThanOrderByIdAsc(Long branchId, Long id);
}
