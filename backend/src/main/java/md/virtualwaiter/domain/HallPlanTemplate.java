package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "hall_plan_templates")
public class HallPlanTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "hall_id", nullable = false)
  public Long hallId;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
  public String payloadJson;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt = Instant.now();
}
