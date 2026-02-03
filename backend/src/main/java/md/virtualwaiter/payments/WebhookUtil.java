package md.virtualwaiter.payments;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

final class WebhookUtil {
  private WebhookUtil() {}

  static String hmacSha256Hex(String secret, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(raw.length * 2);
      for (byte b : raw) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign webhook", e);
    }
  }

  static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) return false;
    int res = 0;
    for (int i = 0; i < a.length(); i++) {
      res |= a.charAt(i) ^ b.charAt(i);
    }
    return res == 0;
  }
}
