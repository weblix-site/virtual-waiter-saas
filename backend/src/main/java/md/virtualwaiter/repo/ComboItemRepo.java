package md.virtualwaiter.repo;

import md.virtualwaiter.domain.ComboItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ComboItemRepo extends CrudRepository<ComboItem, Long> {
  List<ComboItem> findByComboIdOrderBySortOrderAscIdAsc(long comboId);
  List<ComboItem> findByComboIdIn(List<Long> comboIds);
  void deleteByComboId(long comboId);
}
