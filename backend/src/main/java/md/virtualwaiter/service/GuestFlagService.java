package md.virtualwaiter.service;

import md.virtualwaiter.domain.GuestFlag;
import md.virtualwaiter.domain.GuestFlagAudit;
import md.virtualwaiter.repo.GuestFlagAuditRepo;
import md.virtualwaiter.repo.GuestFlagRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GuestFlagService {
  private final GuestFlagRepo repo;
  private final GuestFlagAuditRepo auditRepo;

  public GuestFlagService(GuestFlagRepo repo, GuestFlagAuditRepo auditRepo) {
    this.repo = repo;
    this.auditRepo = auditRepo;
  }

  public record GuestFlags(boolean vip, boolean noShow, boolean conflict) {}

  public GuestFlags getFlags(String phoneE164, Long branchId) {
    List<GuestFlag> rows = repo.findByPhoneFiltered(phoneE164, branchId);
    boolean vip = false;
    boolean noShow = false;
    boolean conflict = false;
    for (GuestFlag g : rows) {
      if (!g.isActive) continue;
      String t = g.flagType;
      if ("VIP".equalsIgnoreCase(t)) vip = true;
      else if ("NO_SHOW".equalsIgnoreCase(t)) noShow = true;
      else if ("CONFLICT".equalsIgnoreCase(t)) conflict = true;
    }
    return new GuestFlags(vip, noShow, conflict);
  }

  @Transactional
  public void setFlags(String phoneE164, Long branchId, Map<String, Boolean> flags, Long changedByStaffId) {
    Instant now = Instant.now();
    for (Map.Entry<String, Boolean> e : flags.entrySet()) {
      String flagType = e.getKey();
      boolean active = e.getValue() != null && e.getValue();
      GuestFlag row = repo.findOne(phoneE164, branchId, flagType).orElse(null);
      Boolean oldActive = row == null ? null : row.isActive;
      if (row == null) {
        if (!active) continue;
        row = new GuestFlag();
        row.phoneE164 = phoneE164;
        row.branchId = branchId;
        row.flagType = flagType;
        row.createdAt = now;
      }
      row.isActive = active;
      row.updatedAt = now;
      repo.save(row);

      if (oldActive == null || oldActive != active) {
        GuestFlagAudit audit = new GuestFlagAudit();
        audit.phoneE164 = phoneE164;
        audit.branchId = branchId;
        audit.flagType = flagType;
        audit.oldActive = oldActive;
        audit.newActive = active;
        audit.changedByStaffId = changedByStaffId;
        audit.changedAt = now;
        auditRepo.save(audit);
      }
    }
  }

  public List<GuestFlagAudit> history(String phoneE164, Long branchId) {
    return branchId == null
      ? auditRepo.findTop200ByPhoneE164OrderByIdDesc(phoneE164)
      : auditRepo.findTop200ByPhoneE164AndBranchIdOrderByIdDesc(phoneE164, branchId);
  }

  public static Map<String, Boolean> toMap(boolean vip, boolean noShow, boolean conflict) {
    Map<String, Boolean> out = new HashMap<>();
    out.put("VIP", vip);
    out.put("NO_SHOW", noShow);
    out.put("CONFLICT", conflict);
    return out;
  }
}
