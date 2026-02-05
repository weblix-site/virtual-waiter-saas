package md.virtualwaiter.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockOtpProvider implements OtpProvider {
  private static final Logger log = LoggerFactory.getLogger(MockOtpProvider.class);

  @Override
  public OtpDeliveryResult sendOtp(String phoneE164, String message, String channel) {
    // DEV: log to console. In prod, replace with SMS/WhatsApp/Telegram provider.
    log.info("[MOCK OTP] channel={} to={} msg={}", channel, phoneE164, message);
    return OtpDeliveryResult.sent("mock");
  }
}
