package md.virtualwaiter.service;

import md.virtualwaiter.config.BillProperties;
import md.virtualwaiter.otp.OtpProperties;
import md.virtualwaiter.config.TipsProperties;
import md.virtualwaiter.domain.BranchSettings;
import md.virtualwaiter.repo.BranchSettingsRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    int otpResendCooldownSeconds,
    int otpLength,
    boolean otpDevEchoCode,
    boolean enablePartyPin,
    boolean allowPayOtherGuestsItems,
    boolean allowPayWholeTable,
    boolean tipsEnabled,
    List<Integer> tipsPercentages,
    boolean payCashEnabled,
    boolean payTerminalEnabled,
    String currencyCode,
    String defaultLang
  ) {}

  public Resolved resolveForBranch(long branchId) {
    BranchSettings s = repo.findById(branchId).orElse(null);
    return new Resolved(
      boolOr(s == null ? null : s.requireOtpForFirstOrder, otpDefaults.requireForFirstOrder),
      intOr(s == null ? null : s.otpTtlSeconds, otpDefaults.ttlSeconds),
      intOr(s == null ? null : s.otpMaxAttempts, otpDefaults.maxAttempts),
      intOr(s == null ? null : s.otpResendCooldownSeconds, otpDefaults.resendCooldownSeconds),
      intOr(s == null ? null : s.otpLength, otpDefaults.length),
      boolOr(s == null ? null : s.otpDevEchoCode, otpDefaults.devEchoCode),
      boolOr(s == null ? null : s.enablePartyPin, true),
      boolOr(s == null ? null : s.allowPayOtherGuestsItems, billDefaults.allowPayOtherGuestsItems),
      boolOr(s == null ? null : s.allowPayWholeTable, billDefaults.allowPayWholeTable),
      boolOr(s == null ? null : s.tipsEnabled, tipsDefaults.enabled),
      listOr(s == null ? null : s.tipsPercentages, tipsDefaults.percentages),
      boolOr(s == null ? null : s.payCashEnabled, true),
      boolOr(s == null ? null : s.payTerminalEnabled, true),
      strOr(s == null ? null : s.currencyCode, "MDL"),
      strOr(s == null ? null : s.defaultLang, "ru")
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
