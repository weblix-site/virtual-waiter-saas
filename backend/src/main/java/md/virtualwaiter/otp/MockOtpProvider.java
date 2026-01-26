package md.virtualwaiter.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockOtpProvider implements OtpProvider {
  private static final Logger log = LoggerFactory.getLogger(MockOtpProvider.class);

  @Override
  public void sendOtp(String phoneE164, String message) {
    // DEV: log to console. In prod, replace with SMS.MD provider.
    log.info("[MOCK OTP] to={} msg={}", phoneE164, message);
  }
}
