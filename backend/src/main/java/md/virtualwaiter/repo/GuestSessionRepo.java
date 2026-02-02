package md.virtualwaiter.repo;

import md.virtualwaiter.domain.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestSessionRepo extends JpaRepository<GuestSession, Long> {
  List<GuestSession> findByPartyId(Long partyId);
  List<GuestSession> findByTableIdOrderByIdDesc(Long tableId);
  List<GuestSession> findByPartyIdIn(List<Long> partyIds);
}
