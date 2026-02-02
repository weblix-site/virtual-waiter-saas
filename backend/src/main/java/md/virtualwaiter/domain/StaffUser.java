package md.virtualwaiter.domain;

import jakarta.persistence.*;

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

  /** WAITER | KITCHEN | ADMIN | SUPER_ADMIN */
  @Column(nullable = false)
  public String role;

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
}
