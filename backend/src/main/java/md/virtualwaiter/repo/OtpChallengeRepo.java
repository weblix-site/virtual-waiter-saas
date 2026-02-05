package md.virtualwaiter.repo;

import md.virtualwaiter.domain.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpChallengeRepo extends JpaRepository<OtpChallenge, Long> {
  Optional<OtpChallenge> findTopByGuestSessionIdAndStatusOrderByCreatedAtDesc(Long guestSessionId, String status);
  long countByGuestSessionIdAndCreatedAtAfter(Long guestSessionId, Instant createdAfter);
}
