package md.virtualwaiter.repo;

import md.virtualwaiter.domain.TableParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface TablePartyRepo extends JpaRepository<TableParty, Long> {

  @Query("select p from TableParty p where p.tableId = ?1 and p.pin = ?2 and p.status = 'ACTIVE' and p.expiresAt > ?3")
  Optional<TableParty> findActiveByTableIdAndPin(Long tableId, String pin, Instant now);

  @Query("select p from TableParty p where p.tableId = ?1 and p.status = 'ACTIVE' and p.expiresAt > ?2")
  Optional<TableParty> findActiveByTableId(Long tableId, Instant now);

  java.util.List<TableParty> findByStatus(String status);
}
