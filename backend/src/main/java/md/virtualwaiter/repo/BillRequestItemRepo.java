package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BillRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillRequestItemRepo extends JpaRepository<BillRequestItem, Long> {
  List<BillRequestItem> findByBillRequestId(Long billRequestId);
}
