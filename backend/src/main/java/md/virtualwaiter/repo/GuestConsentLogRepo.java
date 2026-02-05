package md.virtualwaiter.repo;

import md.virtualwaiter.domain.GuestConsentLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuestConsentLogRepo extends JpaRepository<GuestConsentLog, Long> {
  @Query("""
    select g from GuestConsentLog g
    where g.phoneE164 = :phone
      and (:branchId is null or g.branchId = :branchId)
      and (:consentType is null or g.consentType = :consentType)
      and (:accepted is null or g.accepted = :accepted)
    """)
  List<GuestConsentLog> findByPhoneFiltered(
    @Param("phone") String phone,
    @Param("branchId") Long branchId,
    @Param("consentType") String consentType,
    @Param("accepted") Boolean accepted,
    Pageable pageable
  );

  long deleteByCreatedAtBefore(java.time.Instant createdAt);
}
