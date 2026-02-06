package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuTagRepo extends JpaRepository<MenuTag, Long> {
  List<MenuTag> findByTenantIdOrderByNameAscIdAsc(Long tenantId);
  Optional<MenuTag> findByTenantIdAndSlug(Long tenantId, String slug);
}
