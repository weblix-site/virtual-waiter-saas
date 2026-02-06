package md.virtualwaiter.repo;

import md.virtualwaiter.domain.BranchRecommendationTemplateItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BranchRecommendationTemplateItemRepo extends CrudRepository<BranchRecommendationTemplateItem, Long> {
  List<BranchRecommendationTemplateItem> findByTemplateIdOrderBySortOrderAscIdAsc(long templateId);
  List<BranchRecommendationTemplateItem> findByTemplateIdIn(List<Long> templateIds);
  void deleteByTemplateId(long templateId);
}
