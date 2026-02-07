package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchFeatureFlagRepo extends JpaRepository<BranchFeatureFlag, Long> {
  List<BranchFeatureFlag> findByBranchId(Long branchId);
  Optional<BranchFeatureFlag> findByBranchIdAndFlagId(Long branchId, Long flagId);
}
