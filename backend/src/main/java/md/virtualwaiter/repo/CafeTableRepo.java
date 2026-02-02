package md.virtualwaiter.repo;

import md.virtualwaiter.domain.CafeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CafeTableRepo extends JpaRepository<CafeTable, Long> {
  Optional<CafeTable> findByPublicId(String publicId);
  List<CafeTable> findByBranchId(Long branchId);
  long countByHallId(Long hallId);
}
