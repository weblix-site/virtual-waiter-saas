package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchHall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchHallRepo extends JpaRepository<BranchHall, Long> {
  List<BranchHall> findByBranchIdOrderBySortOrderAscIdAsc(Long branchId);
  List<BranchHall> findByBranchIdAndIsActiveTrueOrderBySortOrderAscIdAsc(Long branchId);
}
