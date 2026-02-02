package md.virtualwaiter.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class LangUtil {
  private LangUtil() {}

  public static Lang current() {
    HttpServletRequest req = currentRequest();
    if (req != null) {
      Lang byHeader = parse(req.getHeader("X-Lang"));
      if (byHeader != null) return byHeader;
      Lang byQuery = parse(req.getParameter("lang"));
      if (byQuery != null) return byQuery;
    }
    return Lang.fromLocale(LocaleContextHolder.getLocale());
  }

  private static Lang parse(String raw) {
    if (raw == null) return null;
    String v = raw.trim().toLowerCase();
    return switch (v) {
      case "ru", "ru-ru" -> Lang.RU;
      case "ro", "ro-ro" -> Lang.RO;
      case "en", "en-us", "en-gb" -> Lang.EN;
      default -> null;
    };
  }

  private static HttpServletRequest currentRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return null;
    return attrs.getRequest();
  }
}
