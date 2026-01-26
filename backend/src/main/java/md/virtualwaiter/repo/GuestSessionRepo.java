package md.virtualwaiter.repo;

import md.virtualwaiter.domain.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestSessionRepo extends JpaRepository<GuestSession, Long> {}
