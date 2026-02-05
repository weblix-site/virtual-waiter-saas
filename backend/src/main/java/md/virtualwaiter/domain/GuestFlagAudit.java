package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_flag_audit")
public class GuestFlagAudit {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "phone_e164", nullable = false)
  public String phoneE164;

  @Column(name = "branch_id")
  public Long branchId;

  @Column(name = "flag_type", nullable = false)
  public String flagType;

  @Column(name = "old_active")
  public Boolean oldActive;

  @Column(name = "new_active", nullable = false)
  public boolean newActive;

  @Column(name = "changed_by_staff_id")
  public Long changedByStaffId;

  @Column(name = "changed_at", nullable = false)
  public Instant changedAt;
}
