package md.virtualwaiter.repo;

import md.virtualwaiter.domain.StaffReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StaffReviewRepo extends JpaRepository<StaffReview, Long> {
  List<StaffReview> findByBranchIdOrderByIdDesc(Long branchId);
  List<StaffReview> findByBranchIdAndStaffUserIdOrderByIdDesc(Long branchId, Long staffUserId);
  Optional<StaffReview> findByGuestSessionId(Long guestSessionId);

  @Query("select avg(r.rating) from StaffReview r where r.staffUserId = :staffUserId")
  Double averageRating(@Param("staffUserId") Long staffUserId);

  long countByStaffUserId(Long staffUserId);
}
