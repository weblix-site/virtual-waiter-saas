package md.virtualwaiter.repo;

import java.util.Optional;
import md.virtualwaiter.domain.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyAccountRepo extends JpaRepository<LoyaltyAccount, Long> {
  Optional<LoyaltyAccount> findByBranchIdAndPhone(Long branchId, String phone);
}
