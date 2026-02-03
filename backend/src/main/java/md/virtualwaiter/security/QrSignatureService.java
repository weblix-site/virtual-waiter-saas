package md.virtualwaiter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Fail fast when secret is missing")
public class QrSignatureService {

  private final byte[] secret;
  private final long ttlSeconds;
  private final long allowedSkewSeconds;
  private final boolean allowLegacySig;

  public QrSignatureService(
    @Value("${app.qr.hmacSecret}") String hmacSecret,
    @Value("${app.qr.signatureTtlSeconds:300}") long ttlSeconds,
    @Value("${app.qr.allowedClockSkewSeconds:60}") long allowedSkewSeconds,
    @Value("${app.qr.allowLegacySig:false}") boolean allowLegacySig
  ) {
    if (hmacSecret == null || hmacSecret.isBlank()) {
      throw new IllegalArgumentException("app.qr.hmacSecret must be set");
    }
    this.secret = hmacSecret.getBytes(StandardCharsets.UTF_8);
    this.ttlSeconds = ttlSeconds;
    this.allowedSkewSeconds = allowedSkewSeconds;
    this.allowLegacySig = allowLegacySig;
  }

  public String signTablePublicId(String tablePublicId, long tsSeconds) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      String payload = tablePublicId + "|" + tsSeconds;
      byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      // base64url without padding
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign tablePublicId", e);
    }
  }

  public boolean verifyTablePublicId(String tablePublicId, String sig, Long tsSeconds) {
    if (tablePublicId == null || sig == null) return false;
    if (tsSeconds == null) {
      return allowLegacySig && constantTimeEquals(signLegacy(tablePublicId), sig);
    }
    long now = java.time.Instant.now().getEpochSecond();
    if (tsSeconds > now + allowedSkewSeconds) return false;
    if (now - tsSeconds > ttlSeconds + allowedSkewSeconds) return false;
    String expected = signTablePublicId(tablePublicId, tsSeconds);
    return constantTimeEquals(expected, sig);
  }

  private String signLegacy(String tablePublicId) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal(tablePublicId.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign tablePublicId", e);
    }
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
