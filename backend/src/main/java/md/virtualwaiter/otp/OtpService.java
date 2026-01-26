package md.virtualwaiter.otp;

import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.OtpChallenge;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.OtpChallengeRepo;
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
  private final SecureRandom rnd = new SecureRandom();
  private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

  public OtpService(OtpProperties props, OtpProvider provider, OtpChallengeRepo otpRepo, GuestSessionRepo sessionRepo) {
    this.props = props;
    this.provider = provider;
    this.otpRepo = otpRepo;
    this.sessionRepo = sessionRepo;
  }

  public record SendResult(long challengeId, int ttlSeconds, String devCode) {}

  public SendResult sendOtp(long guestSessionId, String phoneE164, String lang) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    Optional<OtpChallenge> prev = otpRepo.findTopByGuestSessionIdAndStatusOrderByCreatedAtDesc(guestSessionId, "SENT");
    if (prev.isPresent()) {
      OtpChallenge p = prev.get();
      if (p.expiresAt.isAfter(Instant.now())) {
        long secondsSince = ChronoUnit.SECONDS.between(p.createdAt, Instant.now());
        if (secondsSince < props.resendCooldownSeconds) {
          throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP resend cooldown");
        }
      }
    }

    String code = generateCode(props.length);
    String message = buildMessage(code, lang);

    OtpChallenge c = new OtpChallenge();
    c.guestSessionId = guestSessionId;
    c.phoneE164 = phoneE164;
    c.otpHash = enc.encode(code);
    c.expiresAt = Instant.now().plus(props.ttlSeconds, ChronoUnit.SECONDS);
    c.attemptsLeft = props.maxAttempts;
    c.status = "SENT";
    c.createdAt = Instant.now();
    c = otpRepo.save(c);

    provider.sendOtp(phoneE164, message);

    return new SendResult(c.id, props.ttlSeconds, props.devEchoCode ? code : null);
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

    s.isVerified = true;
    s.verifiedPhone = c.phoneE164;
    sessionRepo.save(s);
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
}
