package md.virtualwaiter.service;

import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.TableParty;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.TablePartyRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PartyService {
  private final TablePartyRepo partyRepo;
  private final GuestSessionRepo sessionRepo;

  public PartyService(TablePartyRepo partyRepo, GuestSessionRepo sessionRepo) {
    this.partyRepo = partyRepo;
    this.sessionRepo = sessionRepo;
  }

  @Scheduled(
    initialDelayString = "${party.close.expired.initial.delay.ms:60000}",
    fixedDelayString = "${party.close.expired.interval.ms:60000}"
  )
  public void scheduledCloseExpired() {
    closeExpiredParties(Instant.now());
  }

  @Transactional
  public int closeExpiredParties(Instant now) {
    List<TableParty> parties = partyRepo.findByStatus("ACTIVE");
    int closed = 0;
    for (TableParty p : parties) {
      if (p.expiresAt.isBefore(now)) {
        closePartyInternal(p, now);
        closed++;
      }
    }
    return closed;
  }

  @Transactional
  public void closeParty(TableParty p, Instant now) {
    if (!"ACTIVE".equals(p.status)) {
      return;
    }
    closePartyInternal(p, now);
  }

  @Transactional
  public TableParty getActivePartyOrNull(GuestSession s, long tableId, Instant now) {
    if (s.partyId == null) return null;
    Optional<TableParty> opt = partyRepo.findById(s.partyId);
    if (opt.isEmpty()) {
      s.partyId = null;
      sessionRepo.save(s);
      return null;
    }
    TableParty p = opt.get();
    if (!Objects.equals(p.tableId, tableId) || !"ACTIVE".equals(p.status) || p.expiresAt.isBefore(now)) {
      s.partyId = null;
      sessionRepo.save(s);
      return null;
    }
    return p;
  }

  private void closePartyInternal(TableParty p, Instant now) {
    p.status = "CLOSED";
    p.closedAt = now;
    partyRepo.save(p);

    List<GuestSession> sessions = sessionRepo.findByPartyId(p.id);
    if (!sessions.isEmpty()) {
      for (GuestSession s : sessions) s.partyId = null;
      sessionRepo.saveAll(sessions);
    }
  }
}
