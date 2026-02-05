package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "branch_settings")
public class BranchSettings {
  @Id
  @Column(name = "branch_id")
  public Long branchId;

  @Column(name = "require_otp_for_first_order")
  public Boolean requireOtpForFirstOrder;

  @Column(name = "otp_ttl_seconds")
  public Integer otpTtlSeconds;

  @Column(name = "otp_max_attempts")
  public Integer otpMaxAttempts;

  @Column(name = "otp_resend_cooldown_seconds")
  public Integer otpResendCooldownSeconds;

  @Column(name = "otp_length")
  public Integer otpLength;

  @Column(name = "otp_dev_echo_code")
  public Boolean otpDevEchoCode;

  @Column(name = "enable_party_pin")
  public Boolean enablePartyPin;

  @Column(name = "allow_pay_other_guests_items")
  public Boolean allowPayOtherGuestsItems;

  @Column(name = "allow_pay_whole_table")
  public Boolean allowPayWholeTable;

  @Column(name = "tips_enabled")
  public Boolean tipsEnabled;

  /** Comma-separated list (e.g., "5,10,15") */
  @Column(name = "tips_percentages")
  public String tipsPercentages;

  @Column(name = "service_fee_percent")
  public Integer serviceFeePercent;

  @Column(name = "tax_percent")
  public Integer taxPercent;

  @Column(name = "inventory_enabled")
  public Boolean inventoryEnabled;

  @Column(name = "loyalty_enabled")
  public Boolean loyaltyEnabled;

  @Column(name = "loyalty_points_per_100cents")
  public Integer loyaltyPointsPer100Cents;

  @Column(name = "online_pay_enabled")
  public Boolean onlinePayEnabled;

  @Column(name = "online_pay_provider")
  public String onlinePayProvider;

  @Column(name = "online_pay_currency_code")
  public String onlinePayCurrencyCode;

  @Column(name = "online_pay_request_url")
  public String onlinePayRequestUrl;

  @Column(name = "online_pay_cacert_path")
  public String onlinePayCacertPath;

  @Column(name = "online_pay_pcert_path")
  public String onlinePayPcertPath;

  @Column(name = "online_pay_pcert_password")
  public String onlinePayPcertPassword;

  @Column(name = "online_pay_key_path")
  public String onlinePayKeyPath;

  @Column(name = "online_pay_redirect_url")
  public String onlinePayRedirectUrl;

  @Column(name = "online_pay_return_url")
  public String onlinePayReturnUrl;

  @Column(name = "pay_cash_enabled")
  public Boolean payCashEnabled;

  @Column(name = "pay_terminal_enabled")
  public Boolean payTerminalEnabled;

  @Column(name = "currency_code")
  public String currencyCode;

  @Column(name = "default_lang")
  public String defaultLang;

  @Column(name = "commission_model")
  public String commissionModel;

  @Column(name = "commission_monthly_fixed_cents")
  public Integer commissionMonthlyFixedCents;

  @Column(name = "commission_monthly_percent")
  public Integer commissionMonthlyPercent;

  @Column(name = "commission_order_percent")
  public Integer commissionOrderPercent;

  @Column(name = "commission_order_fixed_cents")
  public Integer commissionOrderFixedCents;

  @Column(name = "admin_ip_allowlist")
  public String adminIpAllowlist;

  @Column(name = "admin_ip_denylist")
  public String adminIpDenylist;
}
