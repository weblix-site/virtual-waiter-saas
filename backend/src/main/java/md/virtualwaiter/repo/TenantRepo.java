package md.virtualwaiter.repo;

import md.virtualwaiter.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepo extends JpaRepository<Tenant, Long> {
}
