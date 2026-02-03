package md.virtualwaiter.repo;

import md.virtualwaiter.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.Instant;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByBranchIdOrderByIdDesc(Long branchId);
  List<ChatMessage> findByBranchIdAndGuestSessionIdOrderByIdAsc(Long branchId, Long guestSessionId);
  List<ChatMessage> findByBranchIdAndCreatedAtBetweenOrderByIdAsc(Long branchId, Instant from, Instant to);
}
