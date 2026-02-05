package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_profiles")
public class GuestProfile {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "phone_e164", nullable = false, unique = true)
  public String phoneE164;

  @Column(name = "name")
  public String name;

  @Column(name = "preferences")
  public String preferences;

  @Column(name = "allergens")
  public String allergens;

  @Column(name = "visits_count", nullable = false)
  public int visitsCount;

  @Column(name = "first_visit_at")
  public Instant firstVisitAt;

  @Column(name = "last_visit_at")
  public Instant lastVisitAt;
}
