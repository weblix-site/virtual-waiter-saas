package md.virtualwaiter.repo;

import md.virtualwaiter.domain.HallPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HallPlanVersionRepo extends JpaRepository<HallPlanVersion, Long> {
  List<HallPlanVersion> findByPlanIdOrderByCreatedAtDesc(Long planId);
}
