package md.virtualwaiter.repo;

import md.virtualwaiter.domain.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModifierOptionRepo extends JpaRepository<ModifierOption, Long> {
  List<ModifierOption> findByGroupId(Long groupId);
  List<ModifierOption> findByGroupIdIn(List<Long> groupIds);
}
