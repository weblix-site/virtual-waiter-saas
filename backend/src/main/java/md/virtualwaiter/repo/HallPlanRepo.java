package md.virtualwaiter.repo;

import md.virtualwaiter.domain.HallPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallPlanRepo extends JpaRepository<HallPlan, Long> {
  List<HallPlan> findByHallIdOrderBySortOrderAscIdAsc(Long hallId);
  List<HallPlan> findByHallIdAndIsActiveTrueOrderBySortOrderAscIdAsc(Long hallId);
  void deleteByHallId(Long hallId);
}
