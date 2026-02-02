package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BillRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillRequestRepo extends JpaRepository<BillRequest, Long> {
  List<BillRequest> findByStatusOrderByCreatedAtAsc(String status);
  List<BillRequest> findByTableIdAndStatusOrderByCreatedAtAsc(Long tableId, String status);
  BillRequest findTopByGuestSessionIdOrderByCreatedAtDesc(Long guestSessionId);
}
