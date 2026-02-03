package md.virtualwaiter.payments;

import md.virtualwaiter.domain.PaymentIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MaibProvider implements PaymentProvider {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final String webhookSecret;

  public MaibProvider(@Value("${app.payments.maib.webhookSecret:}") String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  @Override
  public String code() {
    return "MAIB";
  }

  @Override
  public ProviderCreateResult create(PaymentIntent intent) {
    throw new UnsupportedOperationException("MAIB provider not configured");
  }

  @Override
  public ProviderCaptureResult capture(PaymentIntent intent) {
    throw new UnsupportedOperationException("MAIB provider not configured");
  }

  @Override
  public ProviderWebhookResult handleWebhook(String body, java.util.Map<String, String> headers) {
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Webhook body is empty");
    }
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new IllegalArgumentException("Webhook secret not configured");
    }
    String sig = headers.getOrDefault("x-signature", headers.getOrDefault("X-Signature", ""));
    String expected = WebhookUtil.hmacSha256Hex(webhookSecret, body);
    if (!WebhookUtil.constantTimeEquals(expected, sig)) {
      throw new IllegalArgumentException("Invalid signature");
    }
    try {
      JsonNode node = MAPPER.readTree(body);
      String ref = node.path("providerRef").asText(null);
      String status = node.path("status").asText(null);
      Integer amount = node.path("amountCents").isNumber() ? node.path("amountCents").asInt() : null;
      String currency = node.path("currencyCode").asText(null);
      if (ref == null || status == null) {
        throw new IllegalArgumentException("providerRef/status required");
      }
      return new ProviderWebhookResult(ref, status.toUpperCase(Locale.ROOT), amount, currency);
    } catch (IOException ex) {
      throw new IllegalArgumentException("Invalid webhook payload");
    }
  }
}
