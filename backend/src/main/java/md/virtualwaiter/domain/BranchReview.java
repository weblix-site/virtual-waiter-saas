package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "branch_reviews")
public class BranchReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  @Column(name = "rating", nullable = false)
  public Integer rating;

  @Column(name = "comment")
  public String comment;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
