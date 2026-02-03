package md.virtualwaiter.repo;

import md.virtualwaiter.domain.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatReadRepo extends JpaRepository<ChatRead, Long> {
  Optional<ChatRead> findByStaffUserIdAndGuestSessionId(Long staffUserId, Long guestSessionId);
}
