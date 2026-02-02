package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepo extends JpaRepository<MenuItem, Long> {
  List<MenuItem> findByCategoryIdInAndIsActiveAndIsStopList(List<Long> categoryIds, boolean isActive, boolean isStopList);
  List<MenuItem> findByCategoryId(Long categoryId);
  List<MenuItem> findByCategoryIdIn(List<Long> categoryIds);

  @Modifying
  @Query("update MenuItem m set m.currency = :currency where m.categoryId in :categoryIds")
  int updateCurrencyByCategoryIds(@Param("currency") String currency, @Param("categoryIds") List<Long> categoryIds);
}
