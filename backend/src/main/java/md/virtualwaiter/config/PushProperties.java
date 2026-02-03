package md.virtualwaiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push")
public class PushProperties {
  /** LOG | FCM_LEGACY */
  public String provider = "LOG";
  /** FCM legacy server key */
  public String fcmServerKey;
  /** FCM legacy API URL */
  public String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
  /** If true, send with dry_run flag */
  public boolean dryRun = false;
  /** Max payload size (bytes) */
  public int maxPayloadBytes = 4096;
}
