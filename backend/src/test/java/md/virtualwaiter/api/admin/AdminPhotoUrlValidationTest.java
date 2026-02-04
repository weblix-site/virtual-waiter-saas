package md.virtualwaiter.api.admin;

import md.virtualwaiter.VirtualWaiterApplication;
import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.BranchSettings;
import md.virtualwaiter.domain.MenuCategory;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.Tenant;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.BranchSettingsRepo;
import md.virtualwaiter.repo.MenuCategoryRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.TenantRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Locale;
import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
  classes = VirtualWaiterApplication.class,
  properties = {
    "spring.datasource.url=jdbc:h2:mem:vwtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "app.qr.hmacSecret=test-secret",
    "app.auth.cookieSecret=test-secret"
  }
)
@AutoConfigureMockMvc
class AdminPhotoUrlValidationTest {
  @Autowired MockMvc mvc;
  @Autowired StaffUserRepo staffUserRepo;
  @Autowired BranchRepo branchRepo;
  @Autowired BranchSettingsRepo branchSettingsRepo;
  @Autowired MenuCategoryRepo categoryRepo;
  @Autowired TenantRepo tenantRepo;
  @Autowired PasswordEncoder passwordEncoder;

  private StaffUser admin;
  private MenuCategory cat;
  private Cookie authCookie;

  @BeforeEach
  void setup() throws Exception {
    staffUserRepo.deleteAll();
    categoryRepo.deleteAll();
    branchSettingsRepo.deleteAll();
    tenantRepo.deleteAll();
    branchRepo.deleteAll();

    Tenant t = new Tenant();
    t.name = "T1";
    t.isActive = true;
    t = tenantRepo.save(t);

    Branch b = new Branch();
    b.tenantId = t.id;
    b.name = "B1";
    b.isActive = true;
    b = branchRepo.save(b);

    BranchSettings s = new BranchSettings();
    s.branchId = b.id;
    s.currencyCode = "MDL";
    branchSettingsRepo.save(s);

    MenuCategory c = new MenuCategory();
    c.branchId = b.id;
    c.nameRu = "Cat";
    c.sortOrder = 0;
    c.isActive = true;
    cat = categoryRepo.save(c);

    StaffUser u = new StaffUser();
    u.branchId = b.id;
    u.username = "admin";
    u.passwordHash = passwordEncoder.encode("pass");
    u.role = "ADMIN";
    u.isActive = true;
    admin = staffUserRepo.save(u);

    authCookie = mvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
      .andReturn()
      .getResponse()
      .getCookie("vw_auth");
    if (authCookie == null) {
      throw new IllegalStateException("Auth cookie not set in login response");
    }
  }

  @Test
  void rejectsBadPhotoUrlScheme() throws Exception {
    String body = String.format(Locale.ROOT,
      "{\"categoryId\": %d, \"nameRu\": \"Item\", \"priceCents\": 100, \"photoUrls\": \"ftp://bad/img.jpg\"}",
      cat.id
    );
    mvc.perform(post("/api/admin/menu/items")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .cookie(authCookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void acceptsGoodPhotoUrl() throws Exception {
    String body = String.format(Locale.ROOT,
      "{\"categoryId\": %d, \"nameRu\": \"Item\", \"priceCents\": 100, \"photoUrls\": \"https://cdn.example.com/img.jpg\"}",
      cat.id
    );
    mvc.perform(post("/api/admin/menu/items")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .cookie(authCookie))
      .andExpect(status().isOk());
  }

  @Test
  void rejectsBadStaffPhotoUrl() throws Exception {
    String body = """
      {"photoUrl": "https://cdn.example.com/photo.bmp"}
      """;
    mvc.perform(patch("/api/admin/staff/" + admin.id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .cookie(authCookie))
      .andExpect(status().isBadRequest());
  }
}
