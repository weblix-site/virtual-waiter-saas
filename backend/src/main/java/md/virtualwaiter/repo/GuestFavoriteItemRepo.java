package md.virtualwaiter.repo;

import java.util.List;
import java.util.Optional;
import md.virtualwaiter.domain.GuestFavoriteItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestFavoriteItemRepo extends JpaRepository<GuestFavoriteItem, Long> {
  Optional<GuestFavoriteItem> findByBranchIdAndPhoneAndMenuItemId(Long branchId, String phone, Long menuItemId);
  List<GuestFavoriteItem> findTop20ByBranchIdAndPhoneOrderByQtyTotalDesc(Long branchId, String phone);
}
