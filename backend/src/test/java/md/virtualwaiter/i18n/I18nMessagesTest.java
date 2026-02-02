package md.virtualwaiter.i18n;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

public class I18nMessagesTest {
  @Test
  void allMessageKeysHaveTranslationsInAllLocales() throws Exception {
    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasename("i18n/messages");
    ms.setDefaultEncoding("UTF-8");

    Set<String> keys = new HashSet<>(extractKeysFromMessages());
    keys.addAll(prefixKeys());

    List<Locale> locales = List.of(new Locale("ru"), new Locale("ro"), new Locale("en"));

    for (String key : keys) {
      for (Locale locale : locales) {
        String msg = ms.getMessage(key, new Object[] {"X"}, null, locale);
        assertNotNull(msg, "Missing message for key: " + key + " locale: " + locale);
        assertNotEquals(key, msg, "Untranslated key: " + key + " locale: " + locale);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static Set<String> extractKeysFromMessages() throws Exception {
    Field f = Messages.class.getDeclaredField("EXACT");
    f.setAccessible(true);
    Map<String, String> exact = (Map<String, String>) f.get(null);
    return new HashSet<>(exact.values());
  }

  private static Set<String> prefixKeys() {
    return Set.of(
      "error.unknown_menu_item",
      "error.missing_required_modifiers",
      "error.unknown_modifier_option",
      "error.modifier_option_not_allowed",
      "error.not_enough_modifiers",
      "error.too_many_modifiers",
      "error.order_item_not_available",
      "error.order_item_missing"
    );
  }
}
