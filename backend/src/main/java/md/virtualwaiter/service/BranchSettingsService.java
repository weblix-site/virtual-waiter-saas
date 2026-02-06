package md.virtualwaiter.service;

import md.virtualwaiter.config.BillProperties;
import md.virtualwaiter.otp.OtpProperties;
import md.virtualwaiter.config.TipsProperties;
import md.virtualwaiter.domain.BranchSettings;
import md.virtualwaiter.repo.BranchSettingsRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.ZoneId;

@Service
public class BranchSettingsService {
  private final BranchSettingsRepo repo;
  private final OtpProperties otpDefaults;
  private final TipsProperties tipsDefaults;
  private final BillProperties billDefaults;

  public BranchSettingsService(
    BranchSettingsRepo repo,
    OtpProperties otpDefaults,
    TipsProperties tipsDefaults,
    BillProperties billDefaults
  ) {
    this.repo = repo;
    this.otpDefaults = otpDefaults;
    this.tipsDefaults = tipsDefaults;
    this.billDefaults = billDefaults;
  }

  public record Resolved(
    boolean requireOtpForFirstOrder,
    int otpTtlSeconds,
    int otpMaxAttempts,
    int otpMaxResends,
    int otpResendCooldownSeconds,
    int otpLength,
    boolean otpDevEchoCode,
    boolean enablePartyPin,
    boolean allowPayOtherGuestsItems,
    boolean allowPayWholeTable,
    boolean tipsEnabled,
    List<Integer> tipsPercentages,
    int serviceFeePercent,
    int taxPercent,
    boolean inventoryEnabled,
    boolean loyaltyEnabled,
    int loyaltyPointsPer100Cents,
    boolean onlinePayEnabled,
    String onlinePayProvider,
    String onlinePayCurrencyCode,
    String onlinePayRequestUrl,
    String onlinePayCacertPath,
    String onlinePayPcertPath,
    String onlinePayPcertPassword,
    String onlinePayKeyPath,
    String onlinePayRedirectUrl,
    String onlinePayReturnUrl,
    boolean payCashEnabled,
    boolean payTerminalEnabled,
    String currencyCode,
    String defaultLang,
    String timeZone,
    String commissionModel,
    int commissionMonthlyFixedCents,
    int commissionMonthlyPercent,
    int commissionOrderPercent,
    int commissionOrderFixedCents,
    String adminIpAllowlist,
    String adminIpDenylist
  ) {}

  public Resolved resolveForBranch(long branchId) {
    BranchSettings s = repo.findById(branchId).orElse(null);
    return new Resolved(
      boolOr(s == null ? null : s.requireOtpForFirstOrder, otpDefaults.requireForFirstOrder),
      intOr(s == null ? null : s.otpTtlSeconds, otpDefaults.ttlSeconds),
      intOr(s == null ? null : s.otpMaxAttempts, otpDefaults.maxAttempts),
      intOr(s == null ? null : s.otpMaxResends, otpDefaults.maxResends),
      intOr(s == null ? null : s.otpResendCooldownSeconds, otpDefaults.resendCooldownSeconds),
      intOr(s == null ? null : s.otpLength, otpDefaults.length),
      boolOr(s == null ? null : s.otpDevEchoCode, otpDefaults.devEchoCode),
      boolOr(s == null ? null : s.enablePartyPin, true),
      boolOr(s == null ? null : s.allowPayOtherGuestsItems, billDefaults.allowPayOtherGuestsItems),
      boolOr(s == null ? null : s.allowPayWholeTable, billDefaults.allowPayWholeTable),
      boolOr(s == null ? null : s.tipsEnabled, tipsDefaults.enabled),
      listOr(s == null ? null : s.tipsPercentages, tipsDefaults.percentages),
      intOr(s == null ? null : s.serviceFeePercent, 0),
      intOr(s == null ? null : s.taxPercent, 0),
      boolOr(s == null ? null : s.inventoryEnabled, false),
      boolOr(s == null ? null : s.loyaltyEnabled, false),
      intOr(s == null ? null : s.loyaltyPointsPer100Cents, 1),
      boolOr(s == null ? null : s.onlinePayEnabled, false),
      strOr(s == null ? null : s.onlinePayProvider, ""),
      strOr(s == null ? null : s.onlinePayCurrencyCode, strOr(s == null ? null : s.currencyCode, "MDL")),
      strOr(s == null ? null : s.onlinePayRequestUrl, ""),
      strOr(s == null ? null : s.onlinePayCacertPath, ""),
      strOr(s == null ? null : s.onlinePayPcertPath, ""),
      strOr(s == null ? null : s.onlinePayPcertPassword, ""),
      strOr(s == null ? null : s.onlinePayKeyPath, ""),
      strOr(s == null ? null : s.onlinePayRedirectUrl, ""),
      strOr(s == null ? null : s.onlinePayReturnUrl, ""),
      boolOr(s == null ? null : s.payCashEnabled, true),
      boolOr(s == null ? null : s.payTerminalEnabled, true),
      strOr(s == null ? null : s.currencyCode, "MDL"),
      strOr(s == null ? null : s.defaultLang, "ru"),
      strOr(s == null ? null : s.timeZone, ZoneId.systemDefault().getId()),
      strOr(s == null ? null : s.commissionModel, "MONTHLY_FIXED"),
      intOr(s == null ? null : s.commissionMonthlyFixedCents, 0),
      intOr(s == null ? null : s.commissionMonthlyPercent, 0),
      intOr(s == null ? null : s.commissionOrderPercent, 0),
      intOr(s == null ? null : s.commissionOrderFixedCents, 0),
      strOr(s == null ? null : s.adminIpAllowlist, ""),
      strOr(s == null ? null : s.adminIpDenylist, "")
    );
  }

  private static boolean boolOr(Boolean v, boolean def) {
    return v != null ? v : def;
  }

  private static int intOr(Integer v, int def) {
    return v != null ? v : def;
  }

  private static List<Integer> listOr(String csv, List<Integer> def) {
    if (csv == null || csv.isBlank()) return def;
    String[] parts = csv.split(",");
    List<Integer> out = new ArrayList<>();
    for (String p : parts) {
      String t = p.trim();
      if (t.isEmpty()) continue;
      try {
        out.add(Integer.parseInt(t));
      } catch (NumberFormatException ignored) {
        // Skip invalid entries
      }
    }
    return out.isEmpty() ? def : out;
  }

  private static String strOr(String v, String def) {
    if (v == null || v.isBlank()) return def;
    return v.trim();
  }
}
