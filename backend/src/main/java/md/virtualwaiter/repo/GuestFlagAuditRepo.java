package md.virtualwaiter.repo;

import md.virtualwaiter.domain.GuestFlagAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestFlagAuditRepo extends JpaRepository<GuestFlagAudit, Long> {
  List<GuestFlagAudit> findTop200ByPhoneE164OrderByIdDesc(String phoneE164);
  List<GuestFlagAudit> findTop200ByPhoneE164AndBranchIdOrderByIdDesc(String phoneE164, Long branchId);
}
