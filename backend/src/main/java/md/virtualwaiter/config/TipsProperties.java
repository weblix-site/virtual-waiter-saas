package md.virtualwaiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app.tips")
public class TipsProperties {
  public boolean enabled = false;
  /** percentages like [5,10,15,20] */
  public List<Integer> percentages = List.of(5,10,15,20);
  /** rounding to cents not used now */
}
