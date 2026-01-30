package md.virtualwaiter.repo;

import md.virtualwaiter.domain.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModifierGroupRepo extends JpaRepository<ModifierGroup, Long> {
  List<ModifierGroup> findByBranchIdOrderByIdAsc(Long branchId);
}
