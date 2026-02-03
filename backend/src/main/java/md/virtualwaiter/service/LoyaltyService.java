package md.virtualwaiter.service;

import java.time.Instant;
import java.util.List;
import md.virtualwaiter.domain.BillRequest;
import md.virtualwaiter.domain.BillRequestItem;
import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.GuestFavoriteItem;
import md.virtualwaiter.domain.GuestOffer;
import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.LoyaltyAccount;
import md.virtualwaiter.domain.LoyaltyPointsLog;
import md.virtualwaiter.domain.MenuItem;
import md.virtualwaiter.domain.OrderItem;
import md.virtualwaiter.repo.BillRequestItemRepo;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestFavoriteItemRepo;
import md.virtualwaiter.repo.GuestOfferRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.LoyaltyAccountRepo;
import md.virtualwaiter.repo.LoyaltyPointsLogRepo;
import md.virtualwaiter.repo.MenuItemRepo;
import md.virtualwaiter.repo.OrderItemRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoyaltyService {
  private final GuestSessionRepo sessionRepo;
  private final CafeTableRepo tableRepo;
  private final BillRequestItemRepo billItemRepo;
  private final OrderItemRepo orderItemRepo;
  private final MenuItemRepo menuItemRepo;
  private final LoyaltyAccountRepo accountRepo;
  private final LoyaltyPointsLogRepo pointsLogRepo;
  private final GuestFavoriteItemRepo favoriteRepo;
  private final GuestOfferRepo offerRepo;
  private final BranchSettingsService settingsService;

  public LoyaltyService(
    GuestSessionRepo sessionRepo,
    CafeTableRepo tableRepo,
    BillRequestItemRepo billItemRepo,
    OrderItemRepo orderItemRepo,
    MenuItemRepo menuItemRepo,
    LoyaltyAccountRepo accountRepo,
    LoyaltyPointsLogRepo pointsLogRepo,
    GuestFavoriteItemRepo favoriteRepo,
    GuestOfferRepo offerRepo,
    BranchSettingsService settingsService
  ) {
    this.sessionRepo = sessionRepo;
    this.tableRepo = tableRepo;
    this.billItemRepo = billItemRepo;
    this.orderItemRepo = orderItemRepo;
    this.menuItemRepo = menuItemRepo;
    this.accountRepo = accountRepo;
    this.pointsLogRepo = pointsLogRepo;
    this.favoriteRepo = favoriteRepo;
    this.offerRepo = offerRepo;
    this.settingsService = settingsService;
  }

  private String normalizePhone(String phone) {
    if (phone == null) return null;
    String trimmed = phone.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @Transactional
  public void applyBillPaid(BillRequest br) {
    if (br == null || br.guestSessionId == null) return;
    GuestSession s = sessionRepo.findById(br.guestSessionId).orElse(null);
    if (s == null) return;
    String phone = normalizePhone(s.verifiedPhone);
    if (phone == null) return;
    CafeTable t = tableRepo.findById(s.tableId).orElse(null);
    if (t == null) return;
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(t.branchId);
    if (!settings.loyaltyEnabled()) return;

    // Points: one log per bill
    if (br.id != null && pointsLogRepo.findByBillRequestId(br.id).isEmpty()) {
      int points = Math.max(0, (br.totalCents / 100) * Math.max(1, settings.loyaltyPointsPer100Cents()));
      if (points > 0) {
        LoyaltyAccount acc = accountRepo.findByBranchIdAndPhone(t.branchId, phone)
          .orElseGet(() -> {
            LoyaltyAccount a = new LoyaltyAccount();
            a.branchId = t.branchId;
            a.phone = phone;
            return a;
          });
        acc.pointsBalance += points;
        acc.updatedAt = Instant.now();
        accountRepo.save(acc);

        LoyaltyPointsLog log = new LoyaltyPointsLog();
        log.branchId = t.branchId;
        log.phone = phone;
        log.billRequestId = br.id;
        log.deltaPoints = points;
        log.reason = "BILL_PAID";
        pointsLogRepo.save(log);
      }
    }

    // Favorites
    List<BillRequestItem> billItems = billItemRepo.findByBillRequestId(br.id);
    for (BillRequestItem bri : billItems) {
      OrderItem oi = orderItemRepo.findById(bri.orderItemId).orElse(null);
      if (oi == null) continue;
      GuestFavoriteItem fi = favoriteRepo.findByBranchIdAndPhoneAndMenuItemId(t.branchId, phone, oi.menuItemId)
        .orElseGet(() -> {
          GuestFavoriteItem f = new GuestFavoriteItem();
          f.branchId = t.branchId;
          f.phone = phone;
          f.menuItemId = oi.menuItemId;
          f.qtyTotal = 0;
          return f;
        });
      fi.qtyTotal += oi.qty;
      fi.lastOrderAt = Instant.now();
      favoriteRepo.save(fi);
    }
  }

  public record LoyaltyProfile(
    String phone,
    int pointsBalance,
    List<FavoriteItemDto> favorites,
    List<OfferDto> offers
  ) {}

  public record FavoriteItemDto(
    long menuItemId,
    String name,
    int qtyTotal
  ) {}

  public record OfferDto(
    long id,
    String title,
    String body,
    String discountCode,
    String startsAt,
    String endsAt,
    boolean isActive
  ) {}

  public LoyaltyProfile getProfile(long branchId, String phone) {
    String norm = normalizePhone(phone);
    if (norm == null) {
      return new LoyaltyProfile(null, 0, List.of(), List.of());
    }
    LoyaltyAccount acc = accountRepo.findByBranchIdAndPhone(branchId, norm).orElse(null);
    int balance = acc == null ? 0 : acc.pointsBalance;
    List<GuestFavoriteItem> favs = favoriteRepo.findTop20ByBranchIdAndPhoneOrderByQtyTotalDesc(branchId, norm);
    List<FavoriteItemDto> favOut = favs.stream().map(f -> {
      MenuItem mi = menuItemRepo.findById(f.menuItemId).orElse(null);
      String name = mi == null ? ("#" + f.menuItemId) : (mi.nameRu != null ? mi.nameRu : mi.nameEn);
      return new FavoriteItemDto(f.menuItemId, name, f.qtyTotal);
    }).toList();

    Instant now = Instant.now();
    List<GuestOffer> offers = offerRepo.findByBranchIdAndPhoneAndIsActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
      branchId, norm, now, now
    );
    List<OfferDto> offerOut = offers.stream().map(o ->
      new OfferDto(o.id, o.title, o.body, o.discountCode,
        o.startsAt == null ? null : o.startsAt.toString(),
        o.endsAt == null ? null : o.endsAt.toString(),
        o.isActive)
    ).toList();
    return new LoyaltyProfile(norm, balance, favOut, offerOut);
  }
}
