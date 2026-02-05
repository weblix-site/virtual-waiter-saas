package md.virtualwaiter.security;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class PermissionUtils {
  private PermissionUtils() {}

  public static String normalize(String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return null;
    Set<String> tokens = new TreeSet<>();
    for (String part : trimmed.split("[,\\s]+")) {
      String t = part.trim();
      if (t.isEmpty()) continue;
      String upper = t.toUpperCase(Locale.ROOT);
      Permission.valueOf(upper);
      tokens.add(upper);
    }
    if (tokens.isEmpty()) return null;
    return String.join(",", tokens);
  }

  public static Set<Permission> parseLenient(String raw) {
    Set<Permission> out = EnumSet.noneOf(Permission.class);
    if (raw == null || raw.isBlank()) return out;
    for (String part : raw.split("[,\\s]+")) {
      String t = part.trim();
      if (t.isEmpty()) continue;
      try {
        out.add(Permission.valueOf(t.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
        // ignore invalid tokens in stored data
      }
    }
    return out;
  }
}
