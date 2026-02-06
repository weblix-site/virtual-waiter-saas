package md.virtualwaiter.repo;

import md.virtualwaiter.domain.Combo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ComboRepo extends CrudRepository<Combo, Long> {
  List<Combo> findByBranchId(long branchId);
  List<Combo> findByBranchIdAndIsActiveTrue(long branchId);
  Optional<Combo> findByBranchIdAndMenuItemId(long branchId, long menuItemId);
}
