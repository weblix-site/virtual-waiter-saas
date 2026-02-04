package md.virtualwaiter.util;

public final class PayloadGuard {
  private PayloadGuard() {}

  public static String truncate(String value, int maxChars) {
    if (value == null) return null;
    if (maxChars <= 0) return "";
    if (value.length() <= maxChars) return value;
    String suffix = "...(truncated)";
    if (maxChars <= suffix.length()) {
      return value.substring(0, maxChars);
    }
    return value.substring(0, maxChars - suffix.length()) + suffix;
  }

  public static String truncateBytes(String value, int maxBytes) {
    if (value == null) return null;
    if (maxBytes <= 0) return "";
    byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if (bytes.length <= maxBytes) return value;
    String suffix = "...(truncated)";
    byte[] suffixBytes = suffix.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    int keep = Math.max(0, maxBytes - suffixBytes.length);
    if (keep <= 0) {
      return new String(bytes, 0, Math.min(maxBytes, bytes.length), java.nio.charset.StandardCharsets.UTF_8);
    }
    String head = new String(bytes, 0, keep, java.nio.charset.StandardCharsets.UTF_8);
    return head + suffix;
  }
}
