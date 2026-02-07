package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "code", nullable = false, unique = true)
  public String code;

  @Column(name = "name", nullable = false)
  public String name;

  @Column(name = "description")
  public String description;

  @Column(name = "default_enabled", nullable = false)
  public boolean defaultEnabled = false;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
