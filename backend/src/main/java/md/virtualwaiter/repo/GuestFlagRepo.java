package md.virtualwaiter.repo;

import md.virtualwaiter.domain.GuestFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuestFlagRepo extends JpaRepository<GuestFlag, Long> {
  @Query("""
    select g from GuestFlag g
    where g.phoneE164 = :phone
      and (:branchId is null or g.branchId = :branchId)
    """)
  List<GuestFlag> findByPhoneFiltered(@Param("phone") String phone, @Param("branchId") Long branchId);

  @Query("""
    select g from GuestFlag g
    where g.phoneE164 = :phone
      and g.flagType = :flagType
      and ((:branchId is null and g.branchId is null) or g.branchId = :branchId)
    """)
  Optional<GuestFlag> findOne(@Param("phone") String phone, @Param("branchId") Long branchId, @Param("flagType") String flagType);
}
