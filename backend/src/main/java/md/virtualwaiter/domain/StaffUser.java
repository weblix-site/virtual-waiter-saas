package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "staff_users")
public class StaffUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id")
  public Long branchId;

  @Column(nullable = false, unique = true)
  public String username;

  @Column(name = "password_hash", nullable = false)
  public String passwordHash;

  /** WAITER | KITCHEN | BAR | HOST | ADMIN | MANAGER | SUPER_ADMIN | OWNER */
  @Column(nullable = false)
  public String role;

  /** Optional extra permissions (CSV of Permission enum values). */
  @Column(name = "permissions")
  public String permissions;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "first_name")
  public String firstName;

  @Column(name = "last_name")
  public String lastName;

  @Column(name = "age")
  public Integer age;

  /** male | female | other */
  @Column(name = "gender")
  public String gender;

  @Column(name = "photo_url")
  public String photoUrl;

  @Column(name = "rating")
  public Integer rating;

  @Column(name = "recommended")
  public Boolean recommended;

  @Column(name = "experience_years")
  public Integer experienceYears;

  @Column(name = "favorite_items")
  public String favoriteItems;

  @Column(name = "shift_started_at")
  public Instant shiftStartedAt;
}
