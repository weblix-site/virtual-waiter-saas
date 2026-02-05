package md.virtualwaiter.otp;

import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.OtpChallenge;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.OtpChallengeRepo;
import md.virtualwaiter.service.BranchSettingsService;
import md.virtualwaiter.service.GuestProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@Service
public class OtpService {
  private final OtpProperties props;
  private final OtpProvider provider;
  private final OtpChallengeRepo otpRepo;
  private final GuestSessionRepo sessionRepo;
  private final CafeTableRepo tableRepo;
  private final BranchSettingsService settingsService;
  private final GuestProfileService guestProfileService;
  private final SecureRandom rnd = new SecureRandom();
  private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

  public OtpService(
    OtpProperties props,
    OtpProvider provider,
    OtpChallengeRepo otpRepo,
    GuestSessionRepo sessionRepo,
    CafeTableRepo tableRepo,
    BranchSettingsService settingsService,
    GuestProfileService guestProfileService
  ) {
    this.props = props;
    this.provider = provider;
    this.otpRepo = otpRepo;
    this.sessionRepo = sessionRepo;
    this.tableRepo = tableRepo;
    this.settingsService = settingsService;
    this.guestProfileService = guestProfileService;
  }

  public record SendResult(long challengeId, int ttlSeconds, String devCode, String deliveryStatus, String deliveryError) {}

  public SendResult sendOtp(long guestSessionId, String phoneE164, String lang, String channel) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    long branchId = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"))
      .branchId;
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(branchId);

    Optional<OtpChallenge> prev = otpRepo.findTopByGuestSessionIdAndStatusOrderByCreatedAtDesc(guestSessionId, "SENT");
    if (prev.isPresent()) {
      OtpChallenge p = prev.get();
      if (p.expiresAt.isAfter(Instant.now())) {
        long secondsSince = ChronoUnit.SECONDS.between(p.createdAt, Instant.now());
        if (secondsSince < settings.otpResendCooldownSeconds()) {
          throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP resend cooldown");
        }
      }
    }
    int maxResends = Math.max(0, settings.otpMaxResends());
    Instant windowStart = Instant.now().minus(settings.otpTtlSeconds(), ChronoUnit.SECONDS);
    long sendCount = otpRepo.countByGuestSessionIdAndCreatedAtAfter(guestSessionId, windowStart);
    if (sendCount >= (1L + maxResends)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP resend limit reached");
    }

    String code = generateCode(settings.otpLength());
    String message = buildMessage(code, lang);
    String channelNorm = normalizeChannel(channel);

    OtpChallenge c = new OtpChallenge();
    c.guestSessionId = guestSessionId;
    c.phoneE164 = phoneE164;
    c.channel = channelNorm;
    c.otpHash = enc.encode(code);
    c.expiresAt = Instant.now().plus(settings.otpTtlSeconds(), ChronoUnit.SECONDS);
    c.attemptsLeft = settings.otpMaxAttempts();
    c.status = "SENT";
    c.deliveryStatus = "SENT";
    c.createdAt = Instant.now();
    c = otpRepo.save(c);

    OtpDeliveryResult delivery = sendWithStatus(phoneE164, message, channelNorm);
    c.deliveryStatus = delivery.status().name();
    c.deliveryProviderRef = delivery.providerRef();
    c.deliveryError = delivery.error();
    c.deliveredAt = delivery.status() == OtpDeliveryStatus.SENT ? Instant.now() : null;
    if (delivery.status() == OtpDeliveryStatus.FAILED) {
      c.status = "FAILED";
    }
    c = otpRepo.save(c);

    return new SendResult(
      c.id,
      settings.otpTtlSeconds(),
      settings.otpDevEchoCode() ? code : null,
      c.deliveryStatus,
      c.deliveryError
    );
  }

  public void verifyOtp(long guestSessionId, long challengeId, String code) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    OtpChallenge c = otpRepo.findById(challengeId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));
    if (!c.guestSessionId.equals(guestSessionId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Challenge does not belong to session");
    }
    if (!"SENT".equals(c.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge not active");
    }
    if (c.expiresAt.isBefore(Instant.now())) {
      c.status = "EXPIRED";
      otpRepo.save(c);
      throw new ResponseStatusException(HttpStatus.GONE, "OTP expired");
    }
    if (c.attemptsLeft <= 0) {
      c.status = "LOCKED";
      otpRepo.save(c);
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP locked");
    }

    boolean ok = enc.matches(code, c.otpHash);
    if (!ok) {
      c.attemptsLeft -= 1;
      if (c.attemptsLeft <= 0) c.status = "LOCKED";
      otpRepo.save(c);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
    }

    c.status = "VERIFIED";
    otpRepo.save(c);

    boolean wasVerified = s.isVerified;
    s.isVerified = true;
    s.verifiedPhone = c.phoneE164;
    sessionRepo.save(s);
    if (!wasVerified) {
      guestProfileService.onVerified(c.phoneE164);
    }
  }

  private String generateCode(int length) {
    int max = 1;
    for (int i = 0; i < length; i++) max *= 10;
    int v = rnd.nextInt(max);
    return String.format("%0" + length + "d", v);
  }

  private String buildMessage(String code, String lang) {
    String l = (lang == null ? "ru" : lang.toLowerCase(Locale.ROOT));
    return switch (l) {
      case "ro" -> "Codul dvs. de confirmare: " + code;
      case "en" -> "Your verification code: " + code;
      default -> "Ваш код подтверждения: " + code;
    };
  }

  private String normalizeChannel(String channel) {
    if (channel == null || channel.isBlank()) return "SMS";
    String c = channel.trim().toUpperCase(Locale.ROOT);
    return switch (c) {
      case "SMS", "WHATSAPP", "TELEGRAM" -> c;
      default -> "SMS";
    };
  }

  private OtpDeliveryResult sendWithStatus(String phoneE164, String message, String channel) {
    try {
      OtpDeliveryResult r = provider.sendOtp(phoneE164, message, channel);
      if (r == null || r.status() == null) {
        return OtpDeliveryResult.failed("Delivery status is null");
      }
      return r;
    } catch (Exception e) {
      return OtpDeliveryResult.failed(e.getMessage());
    }
  }
}
