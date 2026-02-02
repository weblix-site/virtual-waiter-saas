package md.virtualwaiter.i18n;

import java.util.Locale;

public enum Lang {
  RU,
  RO,
  EN;

  public static Lang fromLocale(Locale locale) {
    if (locale == null) return RU;
    String tag = locale.toLanguageTag().toLowerCase(Locale.ROOT);
    if (tag.startsWith("ro") || tag.contains("ro-")) return RO;
    if (tag.startsWith("ru") || tag.contains("ru-")) return RU;
    if (tag.startsWith("en") || tag.contains("en-")) return EN;
    return RU;
  }
}
