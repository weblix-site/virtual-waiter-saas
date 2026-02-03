package md.virtualwaiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push")
public class PushProperties {
  /** LOG | FCM_LEGACY */
  private String provider = "LOG";
  /** FCM legacy server key */
  private String fcmServerKey;
  /** FCM legacy API URL */
  private String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
  /** If true, send with dry_run flag */
  private boolean dryRun = false;
  /** Max payload size (bytes) */
  private int maxPayloadBytes = 4096;

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getFcmServerKey() {
    return fcmServerKey;
  }

  public void setFcmServerKey(String fcmServerKey) {
    this.fcmServerKey = fcmServerKey;
  }

  public String getFcmApiUrl() {
    return fcmApiUrl;
  }

  public void setFcmApiUrl(String fcmApiUrl) {
    this.fcmApiUrl = fcmApiUrl;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public int getMaxPayloadBytes() {
    return maxPayloadBytes;
  }

  public void setMaxPayloadBytes(int maxPayloadBytes) {
    this.maxPayloadBytes = maxPayloadBytes;
  }
}
