package md.virtualwaiter.repo;

import md.virtualwaiter.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByOrderIdIn(List<Long> orderIds);
  List<OrderItem> findByOrderId(Long orderId);
}
