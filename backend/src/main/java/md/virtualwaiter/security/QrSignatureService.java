package md.virtualwaiter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class QrSignatureService {

  private final byte[] secret;

  public QrSignatureService(@Value("${app.qr.hmacSecret}") String hmacSecret) {
    if (hmacSecret == null || hmacSecret.isBlank()) {
      throw new IllegalArgumentException("app.qr.hmacSecret must be set");
    }
    this.secret = hmacSecret.getBytes(StandardCharsets.UTF_8);
  }

  public String signTablePublicId(String tablePublicId) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal(tablePublicId.getBytes(StandardCharsets.UTF_8));
      // base64url without padding
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign tablePublicId", e);
    }
  }

  public boolean verifyTablePublicId(String tablePublicId, String sig) {
    if (tablePublicId == null || sig == null) return false;
    String expected = signTablePublicId(tablePublicId);
    return constantTimeEquals(expected, sig);
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
