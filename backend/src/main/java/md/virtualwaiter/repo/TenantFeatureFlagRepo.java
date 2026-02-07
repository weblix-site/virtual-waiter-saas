package md.virtualwaiter.repo;

import md.virtualwaiter.domain.TenantFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFeatureFlagRepo extends JpaRepository<TenantFeatureFlag, Long> {
  List<TenantFeatureFlag> findByTenantId(Long tenantId);
  Optional<TenantFeatureFlag> findByTenantIdAndFlagId(Long tenantId, Long flagId);
}
