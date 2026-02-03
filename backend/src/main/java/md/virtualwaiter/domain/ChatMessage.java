package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "table_id", nullable = false)
  public Long tableId;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  /** GUEST | STAFF */
  @Column(name = "sender_role", nullable = false)
  public String senderRole;

  @Column(name = "staff_user_id")
  public Long staffUserId;

  @Column(name = "message", nullable = false)
  public String message;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
