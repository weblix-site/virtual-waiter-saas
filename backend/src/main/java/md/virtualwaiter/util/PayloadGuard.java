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
}
