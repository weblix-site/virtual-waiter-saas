package md.virtualwaiter.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import md.virtualwaiter.domain.InventoryItem;
import md.virtualwaiter.domain.MenuItemIngredient;
import md.virtualwaiter.domain.OrderItem;
import md.virtualwaiter.repo.InventoryItemRepo;
import md.virtualwaiter.repo.MenuItemIngredientRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private final InventoryItemRepo inventoryRepo;
  private final MenuItemIngredientRepo ingredientRepo;
  private final BranchSettingsService settingsService;
  private final NotificationEventService notificationEventService;
  private final InventoryAlertService inventoryAlertService;

  public InventoryService(
    InventoryItemRepo inventoryRepo,
    MenuItemIngredientRepo ingredientRepo,
    BranchSettingsService settingsService,
    NotificationEventService notificationEventService,
    InventoryAlertService inventoryAlertService
  ) {
    this.inventoryRepo = inventoryRepo;
    this.ingredientRepo = ingredientRepo;
    this.settingsService = settingsService;
    this.notificationEventService = notificationEventService;
    this.inventoryAlertService = inventoryAlertService;
  }

  @Transactional
  public void applyOrderItems(long branchId, List<OrderItem> items) {
    if (items == null || items.isEmpty()) return;
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(branchId);
    if (!settings.inventoryEnabled()) return;

    Map<Long, Double> consumptionByInventory = new HashMap<>();
    for (OrderItem oi : items) {
      List<MenuItemIngredient> ingredients = ingredientRepo.findByMenuItemId(oi.menuItemId);
      if (ingredients.isEmpty()) continue;
      for (MenuItemIngredient ing : ingredients) {
        double qtyPerItem = ing.qtyPerItem == null ? 0.0 : ing.qtyPerItem;
        double consume = qtyPerItem * oi.qty;
        if (consume <= 0) continue;
        consumptionByInventory.merge(ing.inventoryItemId, consume, Double::sum);
      }
    }

    if (consumptionByInventory.isEmpty()) return;
    List<InventoryItem> toSave = new ArrayList<>();
    for (Map.Entry<Long, Double> e : consumptionByInventory.entrySet()) {
      InventoryItem inv = inventoryRepo.findByIdAndBranchId(e.getKey(), branchId).orElse(null);
      if (inv == null) continue;
      double prevQty = inv.qtyOnHand == null ? 0.0 : inv.qtyOnHand;
      double minQty = inv.minQty == null ? 0.0 : inv.minQty;
      double next = prevQty - e.getValue();
      inv.qtyOnHand = next;
      inv.updatedAt = Instant.now();
      toSave.add(inv);
      if (prevQty > minQty && next <= minQty) {
        notificationEventService.emit(branchId, "inventory_low", inv.id);
        inventoryAlertService.notifyLowStock(branchId, inv, next, minQty);
      }
    }
    if (!toSave.isEmpty()) {
      inventoryRepo.saveAll(toSave);
    }
  }
}
