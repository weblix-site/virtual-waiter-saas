package md.virtualwaiter.service;

import md.virtualwaiter.domain.BillRequest;
import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.Order;
import md.virtualwaiter.domain.WaiterCall;
import md.virtualwaiter.repo.BillRequestRepo;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.BranchSettingsRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlaAlertService {
  private static final Logger log = LoggerFactory.getLogger(SlaAlertService.class);

  private final OrderRepo orderRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final BillRequestRepo billRequestRepo;
  private final CafeTableRepo tableRepo;
  private final BranchSettingsRepo branchSettingsRepo;
  private final StaffPushService pushService;
  private final MessageSource messageSource;

  private final boolean enabled;
  private final int orderCritMin;
  private final int callCritMin;
  private final int billCritMin;
  private final int kitchenCritMin;
  private final int cooldownMinutes;
  private final int lookbackMinutes;

  private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();

  public SlaAlertService(
    OrderRepo orderRepo,
    WaiterCallRepo waiterCallRepo,
    BillRequestRepo billRequestRepo,
    CafeTableRepo tableRepo,
    BranchSettingsRepo branchSettingsRepo,
    StaffPushService pushService,
    MessageSource messageSource,
    @Value("${app.slaAlerts.enabled:true}") boolean enabled,
    @Value("${app.slaAlerts.orderCritMin:10}") int orderCritMin,
    @Value("${app.slaAlerts.callCritMin:5}") int callCritMin,
    @Value("${app.slaAlerts.billCritMin:10}") int billCritMin,
    @Value("${app.slaAlerts.kitchenCritMin:15}") int kitchenCritMin,
    @Value("${app.slaAlerts.cooldownMinutes:5}") int cooldownMinutes,
    @Value("${app.slaAlerts.lookbackMinutes:240}") int lookbackMinutes
  ) {
    this.orderRepo = orderRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.billRequestRepo = billRequestRepo;
    this.tableRepo = tableRepo;
    this.branchSettingsRepo = branchSettingsRepo;
    this.pushService = pushService;
    this.messageSource = messageSource;
    this.enabled = enabled;
    this.orderCritMin = orderCritMin;
    this.callCritMin = callCritMin;
    this.billCritMin = billCritMin;
    this.kitchenCritMin = kitchenCritMin;
    this.cooldownMinutes = cooldownMinutes;
    this.lookbackMinutes = lookbackMinutes;
  }

  @Scheduled(fixedDelayString = "${app.slaAlerts.pollMs:60000}")
  public void run() {
    if (!enabled) return;
    try {
      Instant now = Instant.now();
      Instant cutoff = now.minus(Duration.ofMinutes(lookbackMinutes));

      List<Order> orders = orderRepo.findByStatusInAndCreatedAtAfter(activeOrderStatuses(), cutoff);
      for (Order o : orders) {
        if (o.createdAt == null) continue;
        long ageMin = Duration.between(o.createdAt, now).toMinutes();
        if (ageMin < orderCritMin) continue;
        triggerAlert("ORDER", o.id, o.tableId, ageMin);
      }

      List<Order> kitchenOrders = orderRepo.findByStatusInAndCreatedAtAfter(kitchenStatuses(), cutoff);
      for (Order o : kitchenOrders) {
        if (o.createdAt == null) continue;
        long ageMin = Duration.between(o.createdAt, now).toMinutes();
        if (ageMin < kitchenCritMin) continue;
        triggerAlert("KITCHEN", o.id, o.tableId, ageMin);
      }

      List<WaiterCall> calls = waiterCallRepo.findByStatusInAndCreatedAtAfter(activeCallStatuses(), cutoff);
      for (WaiterCall c : calls) {
        if (c.createdAt == null) continue;
        long ageMin = Duration.between(c.createdAt, now).toMinutes();
        if (ageMin < callCritMin) continue;
        triggerAlert("CALL", c.id, c.tableId, ageMin);
      }

      List<BillRequest> bills = billRequestRepo.findByStatusInAndCreatedAtAfter(activeBillStatuses(), cutoff);
      for (BillRequest b : bills) {
        if (b.createdAt == null) continue;
        long ageMin = Duration.between(b.createdAt, now).toMinutes();
        if (ageMin < billCritMin) continue;
        triggerAlert("BILL", b.id, b.tableId, ageMin);
      }
    } catch (Exception e) {
      log.warn("SLA alert check failed: {}", e.getMessage());
    }
  }

  private void triggerAlert(String alertType, Long refId, Long tableId, long ageMin) {
    if (refId == null || tableId == null) return;
    Instant now = Instant.now();
    String key = alertType.toUpperCase(Locale.ROOT) + ":" + refId;
    Instant last = lastSent.get(key);
    if (last != null && Duration.between(last, now).toMinutes() < cooldownMinutes) return;

    Optional<CafeTable> t = tableRepo.findById(tableId);
    if (t.isEmpty() || t.get().branchId == null) return;

    Map<String, Object> extra = new HashMap<>();
    extra.put("alertType", alertType.toUpperCase(Locale.ROOT));
    extra.put("tableId", tableId);
    extra.put("ageMin", ageMin);
    if (t.get().number != null) {
      extra.put("tableNumber", t.get().number);
    }

    Locale locale = resolveBranchLocale(t.get().branchId);
    String title = messageSource.getMessage("push.sla_alert.title", null, "SLA alert", locale);
    String body = messageSource.getMessage(
      "push.sla_alert." + alertType.toLowerCase(Locale.ROOT),
      new Object[] { t.get().number == null ? "-" : t.get().number, ageMin },
      alertType + " SLA",
      locale
    );
    extra.put("title", title);
    extra.put("body", body);

    pushService.notifyBranch(t.get().branchId, "SLA_ALERT", refId, extra);
    lastSent.put(key, now);
  }

  private Locale resolveBranchLocale(Long branchId) {
    try {
      return branchSettingsRepo.findById(branchId)
        .map(s -> s.defaultLang)
        .filter(l -> l != null && !l.isBlank())
        .map(l -> l.toLowerCase(Locale.ROOT))
        .map(this::toLocale)
        .orElse(Locale.forLanguageTag("ru"));
    } catch (Exception e) {
      return Locale.forLanguageTag("ru");
    }
  }

  private Locale toLocale(String lang) {
    if (lang == null) return Locale.forLanguageTag("ru");
    return switch (lang) {
      case "ro", "ro-md", "ro_md" -> Locale.forLanguageTag("ro");
      case "en", "en-us", "en_us", "en-gb", "en_gb" -> Locale.forLanguageTag("en");
      default -> Locale.forLanguageTag("ru");
    };
  }

  private List<String> activeOrderStatuses() {
    return List.of("NEW", "ACCEPTED", "IN_PROGRESS", "READY", "COOKING");
  }

  private List<String> kitchenStatuses() {
    return List.of("NEW", "ACCEPTED", "IN_PROGRESS");
  }

  private List<String> activeCallStatuses() {
    return List.of("NEW", "ACKNOWLEDGED");
  }

  private List<String> activeBillStatuses() {
    return List.of("CREATED");
  }
}
