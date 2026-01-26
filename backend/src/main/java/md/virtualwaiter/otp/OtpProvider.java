package md.virtualwaiter.otp;

public interface OtpProvider {
  void sendOtp(String phoneE164, String message);
}
