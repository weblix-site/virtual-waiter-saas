package md.virtualwaiter.repo;

import md.virtualwaiter.domain.CafeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface CafeTableRepo extends JpaRepository<CafeTable, Long> {
  Optional<CafeTable> findByPublicId(String publicId);
  List<CafeTable> findByBranchId(Long branchId);
  long countByHallId(Long hallId);
  List<CafeTable> findByIdIn(List<Long> ids);

  @Modifying
  @Transactional
  @Query("update CafeTable t set t.assignedWaiterId = null where t.assignedWaiterId = :waiterId")
  int clearAssignedWaiter(@Param("waiterId") Long waiterId);
}
