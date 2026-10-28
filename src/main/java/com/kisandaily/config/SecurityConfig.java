package com.kisandaily.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisandaily.security.*;
import com.kisandaily.service.AuthService;
import com.kisandaily.util.HttpErrorWriter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      AuthService auth,
      ObjectMapper json,
      @Value("${app.auth-requests-per-minute:20}") int limit)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(c -> {})
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) ->
                            HttpErrorWriter.write(
                                json,
                                res,
                                401,
                                "UNAUTHORIZED",
                                "Session समाप्त है। फिर login करें"))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            HttpErrorWriter.write(
                                json, res, 403, "FORBIDDEN", "इस कार्रवाई की अनुमति नहीं है")))
        .addFilterBefore(
            new AuthRateLimitFilter(json, limit), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new BearerTokenFilter(auth), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
