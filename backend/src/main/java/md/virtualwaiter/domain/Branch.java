package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "branches")
public class Branch {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "tenant_id", nullable = false)
  public Long tenantId;

  @Column(nullable = false)
  public String name;

  @Column(name = "logo_url")
  public String logoUrl;

  @Column(name = "country")
  public String country;

  @Column(name = "address")
  public String address;

  @Column(name = "phone")
  public String phone;

  @Column(name = "contact_person")
  public String contactPerson;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "layout_bg_url")
  public String layoutBgUrl;

  @Column(name = "layout_zones_json")
  public String layoutZonesJson;
}
