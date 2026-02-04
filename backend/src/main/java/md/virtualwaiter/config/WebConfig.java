package md.virtualwaiter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final MediaProperties mediaProperties;

  public WebConfig(MediaProperties mediaProperties) {
    this.mediaProperties = mediaProperties;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path root = Path.of(mediaProperties.storageRoot).toAbsolutePath().normalize();
    String location = root.toUri().toString();
    registry.addResourceHandler("/media/**")
      .addResourceLocations(location)
      .setCachePeriod(3600);
  }
}
