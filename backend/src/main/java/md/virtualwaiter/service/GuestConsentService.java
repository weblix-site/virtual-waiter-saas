package md.virtualwaiter.service;

import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.GuestConsentLog;
import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestConsentLogRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class GuestConsentService {
  private final GuestConsentLogRepo repo;
  private final GuestSessionRepo sessionRepo;
  private final CafeTableRepo tableRepo;

  public GuestConsentService(
    GuestConsentLogRepo repo,
    GuestSessionRepo sessionRepo,
    CafeTableRepo tableRepo
  ) {
    this.repo = repo;
    this.sessionRepo = sessionRepo;
    this.tableRepo = tableRepo;
  }

  public record ConsentRecord(
    String consentType,
    boolean accepted,
    String textVersion,
    String ip,
    String userAgent,
    Instant createdAt
  ) {}

  @Transactional
  public void logConsents(
    long guestSessionId,
    String phoneE164,
    boolean privacyAccepted,
    String privacyVersion,
    boolean marketingAccepted,
    String marketingVersion,
    String ip,
    String userAgent
  ) {
    Long branchId = null;
    GuestSession s = sessionRepo.findById(guestSessionId).orElse(null);
    if (s != null) {
      CafeTable t = tableRepo.findById(s.tableId).orElse(null);
      if (t != null) branchId = t.branchId;
    }

    Instant now = Instant.now();
    List<GuestConsentLog> rows = new ArrayList<>();
    rows.add(buildRow(guestSessionId, phoneE164, branchId, "PRIVACY", privacyAccepted, privacyVersion, ip, userAgent, now));
    rows.add(buildRow(guestSessionId, phoneE164, branchId, "MARKETING", marketingAccepted, marketingVersion, ip, userAgent, now));
    repo.saveAll(rows);
  }

  public List<ConsentRecord> listByPhone(
    String phoneE164,
    Long branchId,
    String consentType,
    Boolean accepted,
    int limit,
    int page
  ) {
    int safeLimit = Math.max(1, Math.min(limit, 500));
    int safePage = Math.max(0, page);
    String ct = consentType == null || consentType.isBlank() ? null : consentType.trim().toUpperCase();
    PageRequest pageable = PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.DESC, "id"));
    List<GuestConsentLog> rows = repo.findByPhoneFiltered(phoneE164, branchId, ct, accepted, pageable);
    List<ConsentRecord> out = new ArrayList<>();
    for (GuestConsentLog r : rows) {
      out.add(new ConsentRecord(
        r.consentType,
        r.accepted,
        r.textVersion,
        r.ip,
        r.userAgent,
        r.createdAt
      ));
    }
    return out;
  }

  private static GuestConsentLog buildRow(
    long guestSessionId,
    String phoneE164,
    Long branchId,
    String consentType,
    boolean accepted,
    String textVersion,
    String ip,
    String userAgent,
    Instant createdAt
  ) {
    GuestConsentLog row = new GuestConsentLog();
    row.guestSessionId = guestSessionId;
    row.phoneE164 = phoneE164;
    row.branchId = branchId;
    row.consentType = consentType;
    row.accepted = accepted;
    row.textVersion = textVersion == null ? "v1" : textVersion.trim();
    row.ip = ip;
    row.userAgent = userAgent;
    row.createdAt = createdAt;
    return row;
  }
}
