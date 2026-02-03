package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  @Column(name="assigned_waiter_id")
  public Long assignedWaiterId;

  @Column(name="hall_id")
  public Long hallId;

  @Column(name="layout_x")
  public Double layoutX;

  @Column(name="layout_y")
  public Double layoutY;

  @Column(name="layout_w")
  public Double layoutW;

  @Column(name="layout_h")
  public Double layoutH;

  @Column(name="layout_shape")
  public String layoutShape;

  @Column(name="layout_rotation")
  public Integer layoutRotation;

  @Column(name="layout_zone")
  public String layoutZone;
}
