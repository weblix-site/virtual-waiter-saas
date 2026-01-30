package md.virtualwaiter.repo;

import md.virtualwaiter.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepo extends JpaRepository<Branch, Long> {
  List<Branch> findByTenantId(Long tenantId);
}
