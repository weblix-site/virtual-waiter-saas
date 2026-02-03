package md.virtualwaiter.payments;

import md.virtualwaiter.domain.PaymentIntent;

public interface PaymentProvider {
  String code();
  ProviderCreateResult create(PaymentIntent intent);
  ProviderCaptureResult capture(PaymentIntent intent);
  default ProviderWebhookResult handleWebhook(String body, java.util.Map<String, String> headers) {
    throw new UnsupportedOperationException(code() + " webhook not configured");
  }

  record ProviderCreateResult(String providerRef, String redirectUrl, String status) {}
  record ProviderCaptureResult(String status) {}
  record ProviderWebhookResult(String providerRef, String status, Integer amountCents, String currencyCode) {}
}
