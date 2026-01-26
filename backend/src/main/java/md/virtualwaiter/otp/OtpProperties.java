package md.virtualwaiter.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {
  /** If true, guest must verify OTP before first order. */
  public boolean requireForFirstOrder = false;
  public int ttlSeconds = 180;
  public int maxAttempts = 5;
  public int resendCooldownSeconds = 60;
  public int length = 4;

  /**
   * DEV only: if true, backend will return the OTP code in API response
   * (to allow testing without spending on SMS). MUST be false in production.
   */
  public boolean devEchoCode = true;
}
