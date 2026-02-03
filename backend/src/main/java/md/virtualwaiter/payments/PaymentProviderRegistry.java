package md.virtualwaiter.payments;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRegistry {
  private final Map<String, PaymentProvider> providers = new HashMap<>();

  public PaymentProviderRegistry(List<PaymentProvider> list) {
    for (PaymentProvider p : list) {
      providers.put(p.code().toUpperCase(Locale.ROOT), p);
    }
  }

  public PaymentProvider resolve(String code) {
    if (code == null) return null;
    return providers.get(code.trim().toUpperCase(Locale.ROOT));
  }
}
