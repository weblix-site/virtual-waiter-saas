package md.virtualwaiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {
  public String storageRoot = "./data/uploads";
  public String publicBaseUrl = "http://localhost:8080";
  public long maxUploadBytes = 5 * 1024 * 1024;
  public long maxVideoBytes = 50 * 1024 * 1024;
  public String allowedPhotoExts = "jpg,jpeg,png,webp,gif";
  public String allowedVideoExts = "mp4,webm,mov";
  public String videoUrlAllowlist = "";
}
