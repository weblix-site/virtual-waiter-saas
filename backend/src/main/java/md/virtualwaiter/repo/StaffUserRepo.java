package md.virtualwaiter.repo;

import md.virtualwaiter.domain.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffUserRepo extends JpaRepository<StaffUser, Long> {
  Optional<StaffUser> findByUsername(String username);
  List<StaffUser> findByBranchId(Long branchId);
}
