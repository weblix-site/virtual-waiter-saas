package md.virtualwaiter.repo;

import md.virtualwaiter.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findTop200ByBranchIdOrderByIdDesc(Long branchId);
}
