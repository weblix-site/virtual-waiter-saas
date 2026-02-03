package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BranchReviewRepo extends JpaRepository<BranchReview, Long> {
  List<BranchReview> findByBranchIdOrderByIdDesc(Long branchId);
  Optional<BranchReview> findByGuestSessionId(Long guestSessionId);
}
