package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name="table_parties")
public class TableParty {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name="table_id", nullable=false)
  public Long tableId;

  @Column(nullable=false, length=4)
  public String pin;

  @Column(nullable=false)
  public String status = "ACTIVE";

  @Column(name="expires_at", nullable=false)
  public Instant expiresAt;

  @Column(name="created_at", nullable=false)
  public Instant createdAt = Instant.now();

  @Column(name="closed_at")
  public Instant closedAt;
}
