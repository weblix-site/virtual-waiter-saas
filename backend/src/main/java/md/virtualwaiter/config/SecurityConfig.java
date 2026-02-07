package md.virtualwaiter.config;

import md.virtualwaiter.repo.StaffUserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import md.virtualwaiter.security.AuthCookieFilter;
import md.virtualwaiter.security.ReadOnlyFilter;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * DB-backed users + httpOnly auth cookie (signed token).
   *
   * Production: keep HTTPS + secure cookie, rotate secrets.
   */
  @Bean
  UserDetailsService userDetailsService(StaffUserRepo staffUserRepo) {
    return username -> {
      var u = staffUserRepo.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
      if (!u.isActive) throw new UsernameNotFoundException("User disabled");
      return User.withUsername(u.username)
        .password(u.passwordHash)
        .roles(u.role)
        .build();
    };
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, AuthCookieFilter authCookieFilter, ReadOnlyFilter readOnlyFilter) throws Exception {
    http.csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/api/public/**",
          "/api/auth/**",
          "/actuator/health/**",
          "/media/**"
        ).permitAll()
        .anyRequest().authenticated()
      )
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .addFilterBefore(authCookieFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(readOnlyFilter, AuthCookieFilter.class);
    return http.build();
  }
}
