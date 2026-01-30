package md.virtualwaiter.repo;

import md.virtualwaiter.domain.StaffDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffDeviceTokenRepo extends JpaRepository<StaffDeviceToken, Long> {
  Optional<StaffDeviceToken> findByToken(String token);
  List<StaffDeviceToken> findByBranchId(Long branchId);
  void deleteByToken(String token);
}
