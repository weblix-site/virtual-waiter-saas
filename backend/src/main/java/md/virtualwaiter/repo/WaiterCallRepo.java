package md.virtualwaiter.repo;

import md.virtualwaiter.domain.WaiterCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;

public interface WaiterCallRepo extends JpaRepository<WaiterCall, Long> {
  List<WaiterCall> findTop100ByTableIdInAndStatusNotOrderByCreatedAtDesc(List<Long> tableIds, String status);
  List<WaiterCall> findByStatusInAndCreatedAtAfter(List<String> statuses, Instant createdAt);
}
