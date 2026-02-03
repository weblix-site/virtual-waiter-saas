package md.virtualwaiter.config;

import md.virtualwaiter.repo.StaffUserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import md.virtualwaiter.security.AuthCookieFilter;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * MVP: DB-backed users + Basic Auth.
   *
   * Production recommendation: switch to JWT (or session auth) behind HTTPS.
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
  SecurityFilterChain filterChain(HttpSecurity http, AuthCookieFilter authCookieFilter) throws Exception {
    http.csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/api/public/**",
          "/api/auth/**",
          "/actuator/health/**"
        ).permitAll()
        .anyRequest().authenticated()
      )
      .httpBasic(Customizer.withDefaults())
      .addFilterBefore(authCookieFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
