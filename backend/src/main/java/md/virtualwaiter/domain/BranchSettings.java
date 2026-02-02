package md.virtualwaiter.domain;

import jakarta.persistence.*;

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

  @Column(name = "pay_cash_enabled")
  public Boolean payCashEnabled;

  @Column(name = "pay_terminal_enabled")
  public Boolean payTerminalEnabled;

  @Column(name = "currency_code")
  public String currencyCode;
}
