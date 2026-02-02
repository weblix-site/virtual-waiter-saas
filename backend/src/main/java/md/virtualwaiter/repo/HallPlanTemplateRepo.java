package md.virtualwaiter.repo;

import md.virtualwaiter.domain.HallPlanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallPlanTemplateRepo extends JpaRepository<HallPlanTemplate, Long> {
  List<HallPlanTemplate> findByBranchIdAndHallIdOrderByUpdatedAtDesc(Long branchId, Long hallId);
  HallPlanTemplate findTopByBranchIdAndHallIdAndNameIgnoreCase(Long branchId, Long hallId, String name);
}
