package md.virtualwaiter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Fail fast when secret is missing")
public class AuthTokenService {
  private final byte[] secret;

  public AuthTokenService(@Value("${app.auth.cookieSecret}") String cookieSecret) {
    if (cookieSecret == null || cookieSecret.isBlank()) {
      throw new IllegalArgumentException("app.auth.cookieSecret must be set");
    }
    this.secret = cookieSecret.getBytes(StandardCharsets.UTF_8);
  }

  public String mint(String username, long ttlSeconds) {
    long exp = Instant.now().getEpochSecond() + Math.max(1, ttlSeconds);
    String payload = username + "|" + exp;
    String sig = hmac(payload);
    return base64Url(payload) + "." + sig;
  }

  public String verify(String token) {
    if (token == null || token.isBlank() || !token.contains(".")) return null;
    String[] parts = token.split("\\.", 2);
    if (parts.length != 2) return null;
    String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String sig = parts[1];
    if (!constantTimeEquals(hmac(payload), sig)) return null;
    String[] p = payload.split("\\|", 2);
    if (p.length != 2) return null;
    String username = p[0];
    long exp;
    try {
      exp = Long.parseLong(p[1]);
    } catch (NumberFormatException e) {
      return null;
    }
    if (Instant.now().getEpochSecond() > exp) return null;
    return username;
  }

  private String hmac(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign auth token", e);
    }
  }

  private String base64Url(String raw) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) return false;
    int res = 0;
    for (int i = 0; i < a.length(); i++) {
      res |= a.charAt(i) ^ b.charAt(i);
    }
    return res == 0;
  }
}
