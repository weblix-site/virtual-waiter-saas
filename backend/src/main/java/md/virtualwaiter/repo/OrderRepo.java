package md.virtualwaiter.repo;

import md.virtualwaiter.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Long> {
  List<Order> findTop50ByTableIdOrderByCreatedAtDesc(Long tableId);
  List<Order> findTop50ByTableIdAndStatusNotInOrderByCreatedAtDesc(Long tableId, List<String> statuses);
  List<Order> findTop100ByTableIdInAndStatusNotInOrderByCreatedAtDesc(List<Long> tableIds, List<String> statuses);
  List<Order> findTop200ByTableIdOrderByCreatedAtDesc(Long tableId);
  List<Order> findTop200ByGuestSessionIdOrderByCreatedAtDesc(Long guestSessionId);
  List<Order> findByTableIdOrderByCreatedAtDesc(Long tableId);
  List<Order> findByGuestSessionIdOrderByCreatedAtDesc(Long guestSessionId);
}
