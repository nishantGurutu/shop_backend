package com.kisandaily.controller;

import com.kisandaily.domain.AuthenticatedUser;
import com.kisandaily.dto.*;
import com.kisandaily.response.*;
import com.kisandaily.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return auth.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return auth.login(request);
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return auth.refresh(request.refreshToken());
  }

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
    return auth.me(user.id());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody RefreshTokenRequest request) {
    auth.logout(request.refreshToken());
  }
}
