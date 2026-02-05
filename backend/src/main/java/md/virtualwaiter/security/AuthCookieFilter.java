package md.virtualwaiter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Arrays;

@Component
@Order(1)
public class AuthCookieFilter extends OncePerRequestFilter {
  private final AuthTokenService tokenService;
  private final UserDetailsService userDetailsService;
  private final String cookieName;

  public AuthCookieFilter(
    AuthTokenService tokenService,
    UserDetailsService userDetailsService,
    @Value("${app.auth.cookieName:vw_auth}") String cookieName
  ) {
    this.tokenService = tokenService;
    this.userDetailsService = userDetailsService;
    this.cookieName = cookieName;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || authHeader.isBlank()) {
        String token = readCookie(request, cookieName);
        if (token != null) {
          String username = tokenService.verify(token);
          if (username != null) {
            var user = userDetailsService.loadUserByUsername(username);
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private String readCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    return Arrays.stream(cookies)
      .filter(c -> name.equals(c.getName()))
      .findFirst()
      .map(Cookie::getValue)
      .orElse(null);
  }
}
