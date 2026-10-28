package com.kisandaily.service;

import com.kisandaily.domain.AuthenticatedUser;
import com.kisandaily.dto.*;
import com.kisandaily.response.*;
import java.util.*;

public interface AuthService {
  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  AuthResponse refresh(String refreshToken);

  Optional<AuthenticatedUser> authenticate(String accessToken);

  UserResponse me(UUID userId);

  UserResponse update(UUID userId, ProfileRequest request);

  void logout(String refreshToken);
}
