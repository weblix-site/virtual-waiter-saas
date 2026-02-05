package md.virtualwaiter.service;

import md.virtualwaiter.repo.GuestConsentLogRepo;
import md.virtualwaiter.repo.GuestProfileRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.OrderRepo;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestDataRetentionService {
  private static final Logger LOG = LoggerFactory.getLogger(GuestDataRetentionService.class);

  private final GuestSessionRepo sessionRepo;
  private final GuestConsentLogRepo consentRepo;
  private final GuestProfileRepo profileRepo;
  private final OrderRepo orderRepo;
  private final boolean enabled;
  private final int guestSessionsDays;
  private final int guestConsentsDays;
  private final int guestProfilesDays;
  private final int orderGuestPhoneDays;

  public GuestDataRetentionService(
    GuestSessionRepo sessionRepo,
    GuestConsentLogRepo consentRepo,
    GuestProfileRepo profileRepo,
    OrderRepo orderRepo,
    @Value("${app.retention.enabled:true}") boolean enabled,
    @Value("${app.retention.guestSessionsDays:90}") int guestSessionsDays,
    @Value("${app.retention.guestConsentsDays:365}") int guestConsentsDays,
    @Value("${app.retention.guestProfilesDays:365}") int guestProfilesDays,
    @Value("${app.retention.orderGuestPhoneDays:365}") int orderGuestPhoneDays
  ) {
    this.sessionRepo = sessionRepo;
    this.consentRepo = consentRepo;
    this.profileRepo = profileRepo;
    this.orderRepo = orderRepo;
    this.enabled = enabled;
    this.guestSessionsDays = guestSessionsDays;
    this.guestConsentsDays = guestConsentsDays;
    this.guestProfilesDays = guestProfilesDays;
    this.orderGuestPhoneDays = orderGuestPhoneDays;
  }

  @Scheduled(cron = "${app.retention.cron:0 0 3 * * *}")
  @Transactional
  public void runRetention() {
    if (!enabled) return;
    Instant now = Instant.now();
    Instant sessionsCutoff = now.minus(guestSessionsDays, ChronoUnit.DAYS);
    Instant consentsCutoff = now.minus(guestConsentsDays, ChronoUnit.DAYS);
    Instant profilesCutoff = now.minus(guestProfilesDays, ChronoUnit.DAYS);
    Instant orderPhoneCutoff = now.minus(orderGuestPhoneDays, ChronoUnit.DAYS);

    long sessionsDeleted = sessionRepo.deleteByExpiresAtBefore(sessionsCutoff);
    long consentsDeleted = consentRepo.deleteByCreatedAtBefore(consentsCutoff);
    long profilesDeleted = profileRepo.deleteByLastVisitAtBefore(profilesCutoff);
    int phonesCleared = orderRepo.clearGuestPhoneBefore(orderPhoneCutoff);

    LOG.info(
      "Guest retention: sessionsDeleted={}, consentsDeleted={}, profilesDeleted={}, ordersPhonesCleared={}",
      sessionsDeleted,
      consentsDeleted,
      profilesDeleted,
      phonesCleared
    );
  }
}
