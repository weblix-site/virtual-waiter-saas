package md.virtualwaiter.repo;

import md.virtualwaiter.domain.GuestProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestProfileRepo extends JpaRepository<GuestProfile, Long> {
  Optional<GuestProfile> findByPhoneE164(String phoneE164);
  long deleteByLastVisitAtBefore(java.time.Instant lastVisitAt);
}
