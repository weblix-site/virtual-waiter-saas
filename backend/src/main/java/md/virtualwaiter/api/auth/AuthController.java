package md.virtualwaiter.api.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.security.AuthTokenService;
import md.virtualwaiter.security.TotpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final StaffUserRepo staffUserRepo;
  private final PasswordEncoder passwordEncoder;
  private final AuthTokenService tokenService;
  private final TotpService totpService;
  private final String cookieName;
  private final int cookieMaxAgeSeconds;
  private final boolean cookieSecure;

  public AuthController(
    StaffUserRepo staffUserRepo,
    PasswordEncoder passwordEncoder,
    AuthTokenService tokenService,
    TotpService totpService,
    @Value("${app.auth.cookieName:vw_auth}") String cookieName,
    @Value("${app.auth.cookieMaxAgeSeconds:604800}") int cookieMaxAgeSeconds,
    @Value("${app.auth.cookieSecure:false}") boolean cookieSecure
  ) {
    this.staffUserRepo = staffUserRepo;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.totpService = totpService;
    this.cookieName = cookieName;
    this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
    this.cookieSecure = cookieSecure;
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password, String totpCode) {}

  @PostMapping("/login")
  public void login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
    var user = staffUserRepo.findByUsername(req.username())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    if (!user.isActive) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
    if (!passwordEncoder.matches(req.password(), user.passwordHash)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
    }
    if (requiresTotp(user) && user.totpEnabled) {
      if (req.totpCode() == null || req.totpCode().isBlank()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "TOTP required");
      }
      if (!totpService.verifyCode(user.totpSecret, req.totpCode(), System.currentTimeMillis())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid TOTP");
      }
    }
    String token = tokenService.mint(user.username, cookieMaxAgeSeconds);
    Cookie cookie = new Cookie(cookieName, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(cookieMaxAgeSeconds);
    response.addCookie(cookie);
  }

  private boolean requiresTotp(md.virtualwaiter.domain.StaffUser user) {
    if (user == null || user.role == null) return false;
    String role = user.role.trim().toUpperCase();
    return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
  }

  @PostMapping("/logout")
  public void logout(HttpServletResponse response) {
    Cookie cookie = new Cookie(cookieName, "");
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
