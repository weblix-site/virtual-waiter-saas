package md.virtualwaiter.service;

import md.virtualwaiter.domain.GuestProfile;
import md.virtualwaiter.repo.GuestProfileRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GuestProfileService {
  private final GuestProfileRepo repo;

  public GuestProfileService(GuestProfileRepo repo) {
    this.repo = repo;
  }

  public record Profile(
    String phoneE164,
    String name,
    String preferences,
    String allergens,
    int visitsCount,
    Instant firstVisitAt,
    Instant lastVisitAt
  ) {}

  public Profile getByPhone(String phoneE164) {
    GuestProfile p = repo.findByPhoneE164(phoneE164).orElse(null);
    if (p == null) {
      return new Profile(phoneE164, null, null, null, 0, null, null);
    }
    return toProfile(p);
  }

  @Transactional
  public Profile updateProfile(String phoneE164, String name, String preferences, String allergens) {
    GuestProfile p = repo.findByPhoneE164(phoneE164).orElseGet(() -> {
      GuestProfile n = new GuestProfile();
      n.phoneE164 = phoneE164;
      n.visitsCount = 0;
      return n;
    });
    p.name = trimToNull(name);
    p.preferences = trimToNull(preferences);
    p.allergens = trimToNull(allergens);
    p = repo.save(p);
    return toProfile(p);
  }

  @Transactional
  public void onVerified(String phoneE164) {
    GuestProfile p = repo.findByPhoneE164(phoneE164).orElseGet(() -> {
      GuestProfile n = new GuestProfile();
      n.phoneE164 = phoneE164;
      n.visitsCount = 0;
      return n;
    });
    Instant now = Instant.now();
    if (p.firstVisitAt == null) p.firstVisitAt = now;
    p.lastVisitAt = now;
    p.visitsCount += 1;
    repo.save(p);
  }

  private static Profile toProfile(GuestProfile p) {
    return new Profile(
      p.phoneE164,
      p.name,
      p.preferences,
      p.allergens,
      p.visitsCount,
      p.firstVisitAt,
      p.lastVisitAt
    );
  }

  private static String trimToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}
