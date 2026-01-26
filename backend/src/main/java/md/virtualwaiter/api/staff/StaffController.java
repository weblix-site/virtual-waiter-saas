package md.virtualwaiter.api.staff;

import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.Order;
import md.virtualwaiter.domain.OrderItem;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.WaiterCall;
import md.virtualwaiter.domain.BillRequest;
import md.virtualwaiter.domain.BillRequestItem;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.OrderItemRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import md.virtualwaiter.repo.BillRequestRepo;
import md.virtualwaiter.repo.BillRequestItemRepo;
import md.virtualwaiter.security.QrSignatureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

  private final StaffUserRepo staffUserRepo;
  private final CafeTableRepo tableRepo;
  private final OrderRepo orderRepo;
  private final OrderItemRepo orderItemRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final BillRequestRepo billRequestRepo;
  private final BillRequestItemRepo billRequestItemRepo;
  private final QrSignatureService qrSig;
  private final String publicBaseUrl;

  public StaffController(
    StaffUserRepo staffUserRepo,
    CafeTableRepo tableRepo,
    OrderRepo orderRepo,
    OrderItemRepo orderItemRepo,
    WaiterCallRepo waiterCallRepo,
    BillRequestRepo billRequestRepo,
    BillRequestItemRepo billRequestItemRepo,
    QrSignatureService qrSig,
    @Value("${app.publicBaseUrl:http://localhost:3000}") String publicBaseUrl
  ) {
    this.staffUserRepo = staffUserRepo;
    this.tableRepo = tableRepo;
    this.orderRepo = orderRepo;
    this.orderItemRepo = orderItemRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.billRequestRepo = billRequestRepo;
    this.billRequestItemRepo = billRequestItemRepo;
    this.qrSig = qrSig;
    this.publicBaseUrl = publicBaseUrl;
  }

  private StaffUser current(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    return staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
  }

  public record MeResponse(long id, String username, String role, long branchId) {}

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    StaffUser u = current(auth);
    return new MeResponse(u.id, u.username, u.role, u.branchId);
  }

  public record StaffOrderItemDto(long id, long menuItemId, String name, int unitPriceCents, int qty, String comment) {}
  public record StaffOrderDto(long id, long tableId, int tableNumber, String status, String createdAt, List<StaffOrderItemDto> items) {}

  @GetMapping("/orders/active")
  public List<StaffOrderDto> activeOrders(Authentication auth) {
    StaffUser u = current(auth);
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    if (tableIds.isEmpty()) return List.of();

    List<String> closed = List.of("CLOSED", "CANCELLED");
    List<Order> orders = orderRepo.findTop100ByTableIdInAndStatusNotInOrderByCreatedAtDesc(tableIds, closed);
    if (orders.isEmpty()) return List.of();

    Map<Long, Integer> tableNumberById = new HashMap<>();
    for (CafeTable t : tables) tableNumberById.put(t.id, t.number);

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> items = orderItemRepo.findByOrderIdIn(orderIds);
    Map<Long, List<StaffOrderItemDto>> itemsByOrder = new HashMap<>();
    for (OrderItem it : items) {
      itemsByOrder.computeIfAbsent(it.orderId, k -> new ArrayList<>()).add(
        new StaffOrderItemDto(it.id, it.menuItemId, it.nameSnapshot, it.unitPriceCents, it.qty, it.comment)
      );
    }

    List<StaffOrderDto> out = new ArrayList<>();
    for (Order o : orders) {
      out.add(new StaffOrderDto(
        o.id,
        o.tableId,
        tableNumberById.getOrDefault(o.tableId, 0),
        o.status,
        o.createdAt.toString(),
        itemsByOrder.getOrDefault(o.id, List.of())
      ));
    }
    return out;
  }

  public record UpdateStatusReq(String status) {}

  @PostMapping("/orders/{orderId}/status")
  public void updateStatus(@PathVariable long orderId, @RequestBody UpdateStatusReq req, Authentication auth) {
    StaffUser u = current(auth);
    Order o = orderRepo.findById(orderId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    // simple branch-level authorization
    CafeTable t = tableRepo.findById(o.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(t.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    if (req == null || req.status == null || req.status.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status");
    }
    o.status = req.status.trim().toUpperCase(Locale.ROOT);
    orderRepo.save(o);
  }

  public record StaffWaiterCallDto(long id, long tableId, int tableNumber, String status, String createdAt) {}

  @GetMapping("/waiter-calls/active")
  public List<StaffWaiterCallDto> activeCalls(Authentication auth) {
    StaffUser u = current(auth);
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    if (tableIds.isEmpty()) return List.of();
    Map<Long, Integer> tableNumberById = new HashMap<>();
    for (CafeTable t : tables) tableNumberById.put(t.id, t.number);

    List<WaiterCall> calls = waiterCallRepo.findTop100ByTableIdInAndStatusNotOrderByCreatedAtDesc(tableIds, "CLOSED");
    List<StaffWaiterCallDto> out = new ArrayList<>();
    for (WaiterCall c : calls) {
      out.add(new StaffWaiterCallDto(c.id, c.tableId, tableNumberById.getOrDefault(c.tableId, 0), c.status, c.createdAt.toString()));
    }
    return out;
  }
  // --- QR helper (for admin/staff tooling) ---
  public record SignedTableUrlResponse(String tablePublicId, String sig, String url) {}

  @GetMapping("/tables/{tablePublicId}/signed-url")
  public SignedTableUrlResponse getSignedTableUrl(@PathVariable String tablePublicId) {
    CafeTable table = tableRepo.findByPublicId(tablePublicId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    String sig = qrSig.signTablePublicId(tablePublicId);
    String url = publicBaseUrl + "/t/" + table.publicId + "?sig=" + sig;
    return new SignedTableUrlResponse(table.publicId, sig, url);
  }


  // --- Bill requests (offline payment) ---
  public record StaffBillLine(long orderItemId, String name, int qty, int unitPriceCents, int lineTotalCents) {}
  public record StaffBillRequestDto(
    long billRequestId,
    int tableNumber,
    String paymentMethod,
    String mode,
    String status,
    int subtotalCents,
    Integer tipsPercent,
    int tipsAmountCents,
    int totalCents,
    List<StaffBillLine> items
  ) {}

  @GetMapping("/bill-requests/active")
  public List<StaffBillRequestDto> activeBillRequests() {
    List<BillRequest> reqs = billRequestRepo.findByStatusOrderByCreatedAtAsc("CREATED");
    if (reqs.isEmpty()) return List.of();

    // Load tables
    Map<Long, CafeTable> tables = new HashMap<>();
    for (BillRequest br : reqs) {
      tables.computeIfAbsent(br.tableId, id -> tableRepo.findById(id).orElse(null));
    }

    // For each bill request load items
    List<StaffBillRequestDto> out = new ArrayList<>();
    for (BillRequest br : reqs) {
      CafeTable t = tables.get(br.tableId);
      if (t == null) continue;
      List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
      List<StaffBillLine> lines = new ArrayList<>();
      for (BillRequestItem it : items) {
        OrderItem oi = orderItemRepo.findById(it.orderItemId).orElse(null);
        if (oi == null) continue;
        lines.add(new StaffBillLine(oi.id, oi.nameSnapshot, oi.qty, oi.unitPriceCents, it.lineTotalCents));
      }
      out.add(new StaffBillRequestDto(br.id, t.number, br.paymentMethod, br.mode, br.status, br.subtotalCents, br.tipsPercent, br.tipsAmountCents, br.totalCents, lines));
    }
    return out;
  }

  public record ConfirmPaidResponse(long billRequestId, String status) {}

  @PostMapping("/bill-requests/{id}/confirm-paid")
  public ConfirmPaidResponse confirmPaid(@PathVariable("id") long id, Authentication auth) {
    StaffUser staff = currentUser(auth);
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill request not found"));
    if (!"CREATED".equals(br.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill request is not active");
    }
    br.status = "PAID_CONFIRMED";
    br.confirmedAt = java.time.Instant.now();
    br.confirmedByStaffId = staff.id;
    billRequestRepo.save(br);

    // Close order items
    List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
    for (BillRequestItem it : items) {
      OrderItem oi = orderItemRepo.findById(it.orderItemId).orElse(null);
      if (oi == null) continue;
      oi.isClosed = true;
      oi.closedAt = java.time.Instant.now();
      oi.billRequestId = br.id;
      orderItemRepo.save(oi);
    }

    return new ConfirmPaidResponse(br.id, br.status);
  }

}
