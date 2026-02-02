package md.virtualwaiter.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@RestControllerAdvice
public class LocalizedExceptionHandler {
  private final MessageSource messageSource;

  public LocalizedExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
    String reason = ex.getReason();
    Locale locale = LocaleContextHolder.getLocale();
    String message = reason;
    String code = null;
    Messages.Resolved resolved = Messages.resolve(reason);
    if (resolved != null) {
      message = messageSource.getMessage(resolved.key(), resolved.args(), reason, locale);
      code = resolved.key();
    }
    Map<String, Object> body = new HashMap<>();
    body.put("message", message == null ? "" : message);
    if (code != null) body.put("code", code);
    body.put("status", ex.getStatusCode().value());
    return ResponseEntity.status(ex.getStatusCode()).body(body);
  }
}
