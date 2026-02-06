package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuTimeSlotRepo extends JpaRepository<MenuTimeSlot, Long> {
  List<MenuTimeSlot> findByBranchId(long branchId);
  List<MenuTimeSlot> findByBranchIdAndIsActiveTrue(long branchId);
}
