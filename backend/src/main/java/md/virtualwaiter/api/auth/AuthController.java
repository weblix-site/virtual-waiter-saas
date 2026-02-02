package md.virtualwaiter.api.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.security.AuthTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final StaffUserRepo staffUserRepo;
  private final PasswordEncoder passwordEncoder;
  private final AuthTokenService tokenService;
  private final String cookieName;
  private final int cookieMaxAgeSeconds;
  private final boolean cookieSecure;

  public AuthController(
    StaffUserRepo staffUserRepo,
    PasswordEncoder passwordEncoder,
    AuthTokenService tokenService,
    @Value("${app.auth.cookieName:vw_auth}") String cookieName,
    @Value("${app.auth.cookieMaxAgeSeconds:604800}") int cookieMaxAgeSeconds,
    @Value("${app.auth.cookieSecure:false}") boolean cookieSecure
  ) {
    this.staffUserRepo = staffUserRepo;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.cookieName = cookieName;
    this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
    this.cookieSecure = cookieSecure;
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  @PostMapping("/login")
  public void login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
    var user = staffUserRepo.findByUsername(req.username())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    if (!user.isActive) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
    if (!passwordEncoder.matches(req.password(), user.passwordHash)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
    }
    String token = tokenService.mint(user.username, cookieMaxAgeSeconds);
    Cookie cookie = new Cookie(cookieName, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(cookieMaxAgeSeconds);
    response.addCookie(cookie);
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
