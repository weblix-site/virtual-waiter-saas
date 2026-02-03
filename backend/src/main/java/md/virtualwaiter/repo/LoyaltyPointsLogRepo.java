package md.virtualwaiter.repo;

import java.util.Optional;
import java.util.List;
import md.virtualwaiter.domain.LoyaltyPointsLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyPointsLogRepo extends JpaRepository<LoyaltyPointsLog, Long> {
  Optional<LoyaltyPointsLog> findByBillRequestId(Long billRequestId);
  List<LoyaltyPointsLog> findTop100ByBranchIdAndPhoneOrderByIdDesc(Long branchId, String phone);
}
