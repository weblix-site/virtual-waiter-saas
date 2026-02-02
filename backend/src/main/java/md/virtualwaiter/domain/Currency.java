package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "currencies")
public class Currency {
  @Id
  @Column(length = 8)
  public String code;

  @Column(nullable = false)
  public String name;

  @Column
  public String symbol;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
