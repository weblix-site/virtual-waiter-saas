package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchRecommendationTemplate;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRecommendationTemplateRepo extends CrudRepository<BranchRecommendationTemplate, Long> {
  List<BranchRecommendationTemplate> findByBranchIdOrderBySortOrderAscIdAsc(long branchId);
  List<BranchRecommendationTemplate> findByBranchIdAndIsActiveTrueOrderBySortOrderAscIdAsc(long branchId);
  Optional<BranchRecommendationTemplate> findByIdAndBranchId(long id, long branchId);
}
