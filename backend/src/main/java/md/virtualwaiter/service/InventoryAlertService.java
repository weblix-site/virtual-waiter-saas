package md.virtualwaiter.service;

import md.virtualwaiter.domain.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryAlertService {
  private static final Logger log = LoggerFactory.getLogger(InventoryAlertService.class);

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final RestTemplate restTemplate = new RestTemplate();

  private final boolean inventoryAlertsEnabled;
  private final boolean emailEnabled;
  private final String emailTo;
  private final String emailFrom;
  private final boolean telegramEnabled;
  private final String telegramBotToken;
  private final String telegramChatId;

  public InventoryAlertService(
    ObjectProvider<JavaMailSender> mailSenderProvider,
    @Value("${app.alerts.inventory.enabled:false}") boolean inventoryAlertsEnabled,
    @Value("${app.alerts.email.enabled:false}") boolean emailEnabled,
    @Value("${app.alerts.email.to:}") String emailTo,
    @Value("${app.alerts.email.from:}") String emailFrom,
    @Value("${app.alerts.telegram.enabled:false}") boolean telegramEnabled,
    @Value("${app.alerts.telegram.botToken:}") String telegramBotToken,
    @Value("${app.alerts.telegram.chatId:}") String telegramChatId
  ) {
    this.mailSenderProvider = mailSenderProvider;
    this.inventoryAlertsEnabled = inventoryAlertsEnabled;
    this.emailEnabled = emailEnabled;
    this.emailTo = emailTo;
    this.emailFrom = emailFrom;
    this.telegramEnabled = telegramEnabled;
    this.telegramBotToken = telegramBotToken;
    this.telegramChatId = telegramChatId;
  }

  public void notifyLowStock(long branchId, InventoryItem item, double qty, double minQty) {
    if (!inventoryAlertsEnabled) return;
    String name = item.nameRu != null ? item.nameRu : (item.nameEn != null ? item.nameEn : item.nameRo);
    if (name == null || name.isBlank()) name = "Item #" + item.id;
    String unit = item.unit == null ? "" : item.unit;
    String message = "Low stock: " + name + " — " + qty + " " + unit + " (min " + minQty + "). Branch #" + branchId;

    if (emailEnabled && emailTo != null && !emailTo.isBlank()) {
      JavaMailSender sender = mailSenderProvider.getIfAvailable();
      if (sender == null) {
        log.warn("Inventory alert email enabled but JavaMailSender not configured");
      } else {
        try {
          SimpleMailMessage mail = new SimpleMailMessage();
          if (emailFrom != null && !emailFrom.isBlank()) {
            mail.setFrom(emailFrom);
          }
          mail.setTo(emailTo);
          mail.setSubject("Inventory low stock");
          mail.setText(message);
          sender.send(mail);
        } catch (Exception e) {
          log.warn("Failed to send inventory low stock email: {}", e.getMessage());
        }
      }
    }

    if (telegramEnabled && telegramBotToken != null && !telegramBotToken.isBlank() && telegramChatId != null && !telegramChatId.isBlank()) {
      try {
        String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";
        Map<String, String> payload = new HashMap<>();
        payload.put("chat_id", telegramChatId);
        payload.put("text", message);
        restTemplate.postForObject(url, payload, String.class);
      } catch (Exception e) {
        log.warn("Failed to send inventory low stock telegram: {}", e.getMessage());
      }
    }
  }
}
