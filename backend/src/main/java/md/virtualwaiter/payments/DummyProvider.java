package md.virtualwaiter.payments;

import java.io.IOException;
import java.util.UUID;
import java.util.Locale;
import md.virtualwaiter.domain.PaymentIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DummyProvider implements PaymentProvider {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final String baseUrl;
  private final String webhookSecret;

  public DummyProvider(
    @Value("${app.publicBaseUrl:http://localhost:3000}") String baseUrl,
    @Value("${app.payments.dummy.webhookSecret:}") String webhookSecret
  ) {
    this.baseUrl = baseUrl;
    this.webhookSecret = webhookSecret;
  }

  @Override
  public String code() {
    return "DUMMY";
  }

  @Override
  public ProviderCreateResult create(PaymentIntent intent) {
    String ref = "DUMMY-" + UUID.randomUUID();
    String redirect = baseUrl + "/t/" + intent.tableId + "?payment_intent=" + intent.id;
    return new ProviderCreateResult(ref, redirect, "PENDING");
  }

  @Override
  public ProviderCaptureResult capture(PaymentIntent intent) {
    return new ProviderCaptureResult("PAID");
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
