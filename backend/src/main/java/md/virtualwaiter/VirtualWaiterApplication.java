package md.virtualwaiter;

import md.virtualwaiter.otp.OtpProperties;
import md.virtualwaiter.config.TipsProperties;
import md.virtualwaiter.config.BillProperties;
import md.virtualwaiter.config.PushProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({OtpProperties.class, TipsProperties.class, BillProperties.class, PushProperties.class})
public class VirtualWaiterApplication {
  public static void main(String[] args) {
    SpringApplication.run(VirtualWaiterApplication.class, args);
  }
}
