package md.virtualwaiter;

import md.virtualwaiter.otp.OtpProperties;
import md.virtualwaiter.config.TipsProperties;
import md.virtualwaiter.config.BillProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OtpProperties.class, TipsProperties.class, BillProperties.class})
public class VirtualWaiterApplication {
  public static void main(String[] args) {
    SpringApplication.run(VirtualWaiterApplication.class, args);
  }
}
