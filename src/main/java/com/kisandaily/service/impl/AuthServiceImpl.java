package com.kisandaily.service.impl;

import com.kisandaily.domain.*;
import com.kisandaily.dto.*;
import com.kisandaily.entity.*;
import com.kisandaily.exceptions.ApiException;
import com.kisandaily.repository.*;
import com.kisandaily.response.*;
import com.kisandaily.service.AuthService;
import com.kisandaily.util.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
  private final UserRepository users;
  private final AuthSessionRepository sessions;
  private final PasswordEncoder passwords;
  private final TokenGenerator tokens;
  private final Duration accessLifetime;
  private final Duration refreshLifetime;
  private final String dummyHash;

  public AuthServiceImpl(
      UserRepository users,
      AuthSessionRepository sessions,
      PasswordEncoder passwords,
      TokenGenerator tokens,
      @Value("${app.access-token-minutes:15}") long accessMinutes,
      @Value("${app.refresh-token-days:30}") long refreshDays) {
    if (accessMinutes < 1 || accessMinutes > 60 || refreshDays < 1 || refreshDays > 90)
      throw new IllegalArgumentException("Invalid token lifetime configuration");
    this.users = users;
    this.sessions = sessions;
    this.passwords = passwords;
    this.tokens = tokens;
    this.accessLifetime = Duration.ofMinutes(accessMinutes);
    this.refreshLifetime = Duration.ofDays(refreshDays);
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  private AuthResponse issue(FarmerUser user, UUID family, Instant refreshExpiry, UUID sessionId) {
    Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    TokenPair pair = tokens.pair();
    AuthSession session = new AuthSession();
    session.setId(sessionId);
    session.setFamilyId(family);
    session.setUser(user);
    session.setAccessTokenHash(TokenGenerator.hash(pair.accessToken()));
    session.setRefreshTokenHash(TokenGenerator.hash(pair.refreshToken()));
    Instant accessExpiry = now.plus(accessLifetime);
    session.setAccessExpiresAt(accessExpiry.isBefore(refreshExpiry) ? accessExpiry : refreshExpiry);
    session.setRefreshExpiresAt(refreshExpiry);
    session.setCreatedAt(now);
    sessions.save(session);
    return new AuthResponse(
        pair.accessToken(),
        pair.refreshToken(),
        "Bearer",
        session.getAccessExpiresAt(),
        refreshExpiry,
        UserResponse.from(user));
  }

  private AuthResponse newSession(FarmerUser user) {
    return issue(
        user,
        UUID.randomUUID(),
        Instant.now().plus(refreshLifetime).truncatedTo(java.time.temporal.ChronoUnit.MICROS),
        UUID.randomUUID());
  }

  private static ApiException unauthorized() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session समाप्त है। फिर login करें");
  }

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest r) {
    UserInputValidator.password(r.password());
    if (!r.password().equals(r.confirmPassword()))
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "दोनों passwords एक जैसे होने चाहिए");
    if (users.existsByMobileNumber(r.mobile()) || !legacyUsers(r.mobile()).isEmpty())
      throw new ApiException(
          HttpStatus.CONFLICT,
          "MOBILE_ALREADY_REGISTERED",
          "यह मोबाइल नंबर पहले से registered है। Login करें।");
    FarmerUser u = new FarmerUser();
    u.setUserId(UUID.randomUUID().toString().replace("-", "").substring(0, 30));
    u.setPasswordHash(passwords.encode(r.password()));
    u.setName(UserInputValidator.name(r.name()));
    u.setMobileNumber(r.mobile());
    u.setPhone(r.mobile());
    u.setEmail(r.email());
    u.setVillage(UserInputValidator.text(r.village(), ""));
    u.setDistrict(UserInputValidator.text(r.district(), ""));
    u.setState(UserInputValidator.text(r.state(), "मध्य प्रदेश"));
    u.setLanguage(UserInputValidator.text(r.language(), "हिंदी (Hindi)"));
    u.setLandArea(UserInputValidator.text(r.landArea(), ""));
    if (r.selectedCrops() != null)
      u.setSelectedCrops(
          new ArrayList<>(
              new LinkedHashSet<>(r.selectedCrops().stream().map(String::strip).toList())));
    u.setProfileCompleted(
        !u.getVillage().isEmpty() && !u.getDistrict().isEmpty() && !u.getSelectedCrops().isEmpty());
    users.saveAndFlush(u);
    return newSession(u);
  }

  @Override
  @Transactional
  public AuthResponse login(LoginRequest r) {
    UserInputValidator.password(r.password());
    Optional<FarmerUser> found = users.findByMobileNumber(r.mobile());
    if (found.isEmpty()) {
      List<FarmerUser> legacy = legacyUsers(r.mobile());
      if (legacy.size() == 1) found = Optional.of(legacy.get(0));
    }
    boolean valid =
        passwords.matches(r.password(), found.map(FarmerUser::getPasswordHash).orElse(dummyHash));
    if (found.isEmpty() || !valid)
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "मोबाइल नंबर या password गलत है");
    FarmerUser user = found.get();
    if (user.getMobileNumber() == null) {
      user.setMobileNumber(r.mobile());
      user.setPhone(r.mobile());
      users.saveAndFlush(user);
    }
    return newSession(user);
  }

  private List<FarmerUser> legacyUsers(String mobile) {
    return users.findByPhoneInAndMobileNumberIsNull(
        List.of(mobile, "+91" + mobile, "+91 " + mobile));
  }

  // Commit replay revocation even when returning a 401 response.
  @Override
  @Transactional(noRollbackFor = ApiException.class)
  public AuthResponse refresh(String rawToken) {
    AuthSession previous =
        sessions
            .findRefreshForUpdate(TokenGenerator.hash(rawToken))
            .orElseThrow(AuthServiceImpl::unauthorized);
    Instant now = Instant.now();
    if (previous.getRevokedAt() != null) {
      if (previous.getReplacedBy() != null) sessions.revokeFamily(previous.getFamilyId(), now);
      throw unauthorized();
    }
    if (!previous.getRefreshExpiresAt().isAfter(now)) throw unauthorized();
    UUID nextId = UUID.randomUUID();
    previous.setRevokedAt(now);
    previous.setReplacedBy(nextId);
    return issue(
        previous.getUser(), previous.getFamilyId(), previous.getRefreshExpiresAt(), nextId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AuthenticatedUser> authenticate(String token) {
    if (!TokenGenerator.valid(token)) return Optional.empty();
    return sessions
        .findByAccessTokenHash(TokenGenerator.hash(token))
        .filter(
            s ->
                s.getRevokedAt() == null
                    && s.getAccessExpiresAt().isAfter(Instant.now())
                    && s.getRefreshExpiresAt().isAfter(Instant.now()))
        .map(s -> new AuthenticatedUser(s.getUser().getId(), s.getUser().getUserId(), s.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse me(UUID id) {
    return UserResponse.from(users.findById(id).orElseThrow(AuthServiceImpl::unauthorized));
  }

  @Override
  @Transactional
  public UserResponse update(UUID id, ProfileRequest p) {
    FarmerUser u = users.findById(id).orElseThrow(AuthServiceImpl::unauthorized);
    u.setName(UserInputValidator.name(p.name()));
    if (u.getMobileNumber() != null) {
      if (p.phone() != null
          && !p.phone().isBlank()
          && !UserInputValidator.mobile(p.phone()).equals(u.getMobileNumber())) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "MOBILE_CHANGE_NOT_SUPPORTED",
            "Login मोबाइल नंबर इस profile form से नहीं बदला जा सकता");
      }
      u.setPhone(u.getMobileNumber());
    } else {
      u.setPhone(UserInputValidator.text(p.phone(), ""));
    }
    u.setVillage(p.village().strip());
    u.setDistrict(p.district().strip());
    u.setState(p.state().strip());
    u.setLanguage(p.language().strip());
    u.setLandArea(UserInputValidator.text(p.landArea(), ""));
    u.setSelectedCrops(
        new ArrayList<>(
            new LinkedHashSet<>(p.selectedCrops().stream().map(String::strip).toList())));
    u.setProfileCompleted(true);
    return UserResponse.from(u);
  }

  @Override
  @Transactional
  public void logout(String refreshToken) {
    sessions
        .findRefreshForUpdate(TokenGenerator.hash(refreshToken))
        .ifPresent(s -> sessions.revokeFamily(s.getFamilyId(), Instant.now()));
  }

  @Scheduled(fixedDelayString = "${app.session-cleanup-ms:3600000}", initialDelay = 3600000)
  @Transactional
  public void purgeExpiredSessions() {
    sessions.deleteByRefreshExpiresAtBefore(Instant.now());
  }
}
