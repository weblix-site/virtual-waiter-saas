package md.virtualwaiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bill")
public class BillProperties {
  public boolean allowPayOtherGuestsItems = false;
  public boolean allowPayWholeTable = false;
  public int expireMinutes = 120;
}
