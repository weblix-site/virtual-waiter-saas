package md.virtualwaiter.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpService {
  private static final int SECRET_BYTES = 20;
  private static final int CODE_DIGITS = 6;
  private static final int TIME_STEP_SECONDS = 30;
  private static final String HMAC_ALG = "HmacSHA1";
  private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private final SecureRandom random = new SecureRandom();

  public String generateSecret() {
    byte[] bytes = new byte[SECRET_BYTES];
    random.nextBytes(bytes);
    return base32Encode(bytes);
  }

  public boolean verifyCode(String secret, String code, long nowMillis) {
    if (secret == null || secret.isBlank() || code == null) return false;
    String normalized = code.trim();
    if (!normalized.matches("\\d{6}")) return false;
    long counter = (nowMillis / 1000L) / TIME_STEP_SECONDS;
    for (long offset = -1; offset <= 1; offset++) {
      String expected = generateCode(secret, counter + offset);
      if (expected.equals(normalized)) return true;
    }
    return false;
  }

  public String buildOtpauthUrl(String label, String issuer, String secret) {
    String safeLabel = urlEncode(label == null ? "user" : label);
    String safeIssuer = urlEncode(issuer == null ? "VirtualWaiter" : issuer);
    StringBuilder sb = new StringBuilder();
    sb.append("otpauth://totp/");
    sb.append(safeIssuer).append(":").append(safeLabel);
    sb.append("?secret=").append(secret);
    sb.append("&issuer=").append(safeIssuer);
    sb.append("&period=").append(TIME_STEP_SECONDS);
    sb.append("&digits=").append(CODE_DIGITS);
    return sb.toString();
  }

  private String generateCode(String secret, long counter) {
    byte[] key = base32Decode(secret);
    if (key.length == 0) return "";
    byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
    byte[] hash = hmacSha1(key, data);
    int offset = hash[hash.length - 1] & 0x0f;
    int binary =
      ((hash[offset] & 0x7f) << 24)
        | ((hash[offset + 1] & 0xff) << 16)
        | ((hash[offset + 2] & 0xff) << 8)
        | (hash[offset + 3] & 0xff);
    int otp = binary % (int) Math.pow(10, CODE_DIGITS);
    return String.format(Locale.ROOT, "%0" + CODE_DIGITS + "d", otp);
  }

  private byte[] hmacSha1(byte[] key, byte[] data) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALG);
      mac.init(new SecretKeySpec(key, HMAC_ALG));
      return mac.doFinal(data);
    } catch (Exception e) {
      return new byte[0];
    }
  }

  private String base32Encode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    int buffer = 0;
    int bitsLeft = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xff);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        int index = (buffer >> (bitsLeft - 5)) & 0x1f;
        bitsLeft -= 5;
        sb.append(BASE32_ALPHABET[index]);
      }
    }
    if (bitsLeft > 0) {
      int index = (buffer << (5 - bitsLeft)) & 0x1f;
      sb.append(BASE32_ALPHABET[index]);
    }
    return sb.toString();
  }

  private byte[] base32Decode(String input) {
    if (input == null) return new byte[0];
    String s = input.trim().replace("=", "").toUpperCase(Locale.ROOT);
    ByteBuffer out = ByteBuffer.allocate(s.length() * 5 / 8 + 1);
    int buffer = 0;
    int bitsLeft = 0;
    for (int i = 0; i < s.length(); i++) {
      int val = base32Value(s.charAt(i));
      if (val < 0) continue;
      buffer = (buffer << 5) | val;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        bitsLeft -= 8;
        out.put((byte) ((buffer >> bitsLeft) & 0xff));
      }
    }
    byte[] result = new byte[out.position()];
    out.flip();
    out.get(result);
    return result;
  }

  private int base32Value(char c) {
    if (c >= 'A' && c <= 'Z') return c - 'A';
    if (c >= '2' && c <= '7') return 26 + (c - '2');
    return -1;
  }

  private String urlEncode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
