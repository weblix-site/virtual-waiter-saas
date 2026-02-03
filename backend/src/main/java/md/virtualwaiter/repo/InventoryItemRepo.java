package md.virtualwaiter.repo;

import java.util.List;
import java.util.Optional;
import md.virtualwaiter.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepo extends JpaRepository<InventoryItem, Long> {
  List<InventoryItem> findByBranchIdOrderByIdDesc(long branchId);
  Optional<InventoryItem> findByIdAndBranchId(long id, long branchId);
  List<InventoryItem> findByBranchIdAndIsActiveTrueOrderByIdDesc(long branchId);
  List<InventoryItem> findByBranchIdAndIsActiveTrueAndQtyOnHandLessThanEqualOrderByIdDesc(long branchId, double qtyOnHand);
}
