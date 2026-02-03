package md.virtualwaiter.repo;

import java.util.List;
import java.util.Optional;
import md.virtualwaiter.domain.BranchDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchDiscountRepo extends JpaRepository<BranchDiscount, Long> {
  Optional<BranchDiscount> findFirstByBranchIdAndCodeIgnoreCase(Long branchId, String code);
  List<BranchDiscount> findByBranchIdAndScopeAndActiveTrue(Long branchId, String scope);
  List<BranchDiscount> findByBranchIdOrderByIdDesc(Long branchId);
}
