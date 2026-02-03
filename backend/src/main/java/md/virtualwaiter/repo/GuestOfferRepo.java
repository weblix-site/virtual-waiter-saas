package md.virtualwaiter.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import md.virtualwaiter.domain.GuestOffer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestOfferRepo extends JpaRepository<GuestOffer, Long> {
  List<GuestOffer> findTop100ByBranchIdAndPhoneOrderByIdDesc(Long branchId, String phone);
  List<GuestOffer> findByBranchIdAndPhoneAndIsActiveTrue(Long branchId, String phone);
  Optional<GuestOffer> findByIdAndBranchId(Long id, Long branchId);
  List<GuestOffer> findByBranchIdAndPhoneAndIsActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
    Long branchId,
    String phone,
    Instant startsAt,
    Instant endsAt
  );
}
