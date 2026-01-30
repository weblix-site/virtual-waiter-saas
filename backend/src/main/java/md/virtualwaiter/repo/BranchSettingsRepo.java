package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchSettingsRepo extends JpaRepository<BranchSettings, Long> {
}
