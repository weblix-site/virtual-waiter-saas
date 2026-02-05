package md.virtualwaiter.otp;

public interface OtpProvider {
  OtpDeliveryResult sendOtp(String phoneE164, String message, String channel);
}
