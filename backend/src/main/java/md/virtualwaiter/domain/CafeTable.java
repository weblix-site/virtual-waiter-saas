package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name="tables")
public class CafeTable {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable=false)
  public Integer number;

  @Column(name="public_id", nullable=false, unique=true)
  public String publicId;

  @Column(name="branch_id", nullable=false)
  public Long branchId;
}
