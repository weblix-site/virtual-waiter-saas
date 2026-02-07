package md.virtualwaiter.repo;

import md.virtualwaiter.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureFlagRepo extends JpaRepository<FeatureFlag, Long> {
  Optional<FeatureFlag> findByCodeIgnoreCase(String code);
}
