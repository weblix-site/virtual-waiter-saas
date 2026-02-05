package md.virtualwaiter.otp;

public record OtpDeliveryResult(OtpDeliveryStatus status, String providerRef, String error) {
  public static OtpDeliveryResult sent(String providerRef) {
    return new OtpDeliveryResult(OtpDeliveryStatus.SENT, providerRef, null);
  }

  public static OtpDeliveryResult queued(String providerRef) {
    return new OtpDeliveryResult(OtpDeliveryStatus.QUEUED, providerRef, null);
  }

  public static OtpDeliveryResult failed(String error) {
    return new OtpDeliveryResult(OtpDeliveryStatus.FAILED, null, error);
  }
}
