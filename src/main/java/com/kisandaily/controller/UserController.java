package com.kisandaily.controller;

import com.kisandaily.domain.AuthenticatedUser;
import com.kisandaily.dto.ProfileRequest;
import com.kisandaily.response.UserResponse;
import com.kisandaily.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
  private final AuthService auth;

  public UserController(AuthService auth) {
    this.auth = auth;
  }

  @PutMapping("/profile")
  public UserResponse profile(
      @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ProfileRequest request) {
    return auth.update(user.id(), request);
  }
}
