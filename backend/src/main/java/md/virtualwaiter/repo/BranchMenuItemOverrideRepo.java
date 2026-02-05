package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchMenuItemOverride;
import md.virtualwaiter.domain.BranchMenuItemOverrideId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchMenuItemOverrideRepo extends JpaRepository<BranchMenuItemOverride, BranchMenuItemOverrideId> {
  Optional<BranchMenuItemOverride> findByBranchIdAndMenuItemId(Long branchId, Long menuItemId);
  List<BranchMenuItemOverride> findByBranchIdAndMenuItemIdIn(Long branchId, List<Long> menuItemIds);
  List<BranchMenuItemOverride> findByBranchId(Long branchId);
  void deleteByBranchId(Long branchId);
}
