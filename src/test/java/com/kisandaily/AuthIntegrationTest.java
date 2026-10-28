package com.kisandaily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.kisandaily.entity.*;
import com.kisandaily.repository.*;
import com.kisandaily.util.TokenGenerator;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired AuthSessionRepository sessions;
  @Autowired PasswordEncoder passwords;
  static final String PASSWORD = "secret-pass-123";

  @BeforeEach
  void clear() {
    sessions.deleteAll();
    users.deleteAll();
  }

  String mobile(String label) {
    return "9"
        + String.format(
            "%09d",
            Math.floorMod(label.strip().toLowerCase(Locale.ROOT).hashCode(), 1_000_000_000));
  }

  Map<String, Object> registration(String number) {
    return new HashMap<>(
        Map.of(
            "name",
            "Test Farmer",
            "mobile",
            number,
            "password",
            PASSWORD,
            "confirmPassword",
            PASSWORD));
  }

  ResultActions postJson(String path, Object body) throws Exception {
    return mvc.perform(
        post("/api" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
  }

  JsonNode registered(String label) throws Exception {
    return json.readTree(
        postJson("/auth/register", registration(mobile(label)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode loggedIn(String label) throws Exception {
    return json.readTree(
        postJson("/auth/login", Map.of("mobile", mobile(label), "password", PASSWORD))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  ResultActions refresh(String token) throws Exception {
    return postJson("/auth/refresh", Map.of("refreshToken", token));
  }

  @Test
  void registrationRequiresNameMobilePasswordAndConfirmationButNotEmail() throws Exception {
    JsonNode r = registered("first");
    assertThat(r.at("/user/mobile").asText()).isEqualTo(mobile("first"));
    assertThat(r.at("/user/email").isNull()).isTrue();
    for (String field : List.of("name", "mobile", "password", "confirmPassword")) {
      Map<String, Object> body = registration(mobile("missing-" + field));
      body.remove(field);
      postJson("/auth/register", body).andExpect(status().isBadRequest());
    }
  }

  @Test
  void optionalEmailIsValidatedNormalizedAndReturned() throws Exception {
    var body = registration(mobile("email"));
    body.put("email", " Farmer@Example.com ");
    postJson("/auth/register", body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("user.email").value("farmer@example.com"));
    body = registration(mobile("blank-email"));
    body.put("email", "  ");
    postJson("/auth/register", body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("user.email").isEmpty());
    body = registration(mobile("bad-email"));
    body.put("email", "not-an-email");
    postJson("/auth/register", body).andExpect(status().isBadRequest());
  }

  @Test
  void confirmationMismatchAndInvalidMobileAreRejected() throws Exception {
    var body = registration(mobile("mismatch"));
    body.put("confirmPassword", "different-pass");
    postJson("/auth/register", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("PASSWORD_MISMATCH"));
    for (String number : List.of("", "123", "1234567890", "98765432100", "abcdefghij"))
      postJson("/auth/register", registration(number)).andExpect(status().isBadRequest());
    assertThat(users.count()).isZero();
  }

  @Test
  void normalizedMobileIsUniqueAndCanBeUsedToLogin() throws Exception {
    String number = mobile("normalized");
    var body = registration("+91 " + number.substring(0, 5) + " " + number.substring(5));
    postJson("/auth/register", body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("user.mobile").value(number));
    postJson("/auth/register", registration(number))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("code").value("MOBILE_ALREADY_REGISTERED"));
    postJson("/auth/login", Map.of("mobile", "+91" + number, "password", PASSWORD))
        .andExpect(status().isOk());
    assertThat(users.count()).isEqualTo(1);
  }

  @Test
  void responsesReturnUserAndBothTokensButStoreOnlyHashes() throws Exception {
    JsonNode r = registered("tokens");
    String access = r.get("accessToken").asText();
    String refresh = r.get("refreshToken").asText();
    assertThat(access).hasSize(43).isNotEqualTo(refresh);
    assertThat(r.get("tokenType").asText()).isEqualTo("Bearer");
    assertThat(Instant.parse(r.get("accessTokenExpiresAt").asText()))
        .isBefore(Instant.parse(r.get("refreshTokenExpiresAt").asText()));
    assertThat(r.at("/user/name").asText()).isEqualTo("Test Farmer");
    assertThat(r.at("/user/mobile").asText()).isEqualTo(mobile("tokens"));
    assertThat(r.at("/user/selectedCrops").isArray()).isTrue();
    assertThat(r.at("/user/passwordHash").isMissingNode()).isTrue();
    assertThat(r.at("/user/confirmPassword").isMissingNode()).isTrue();
    assertThat(users.findAll().get(0).getPasswordHash())
        .startsWith("$2a$")
        .doesNotContain("secret-pass");
    AuthSession s = sessions.findAll().get(0);
    assertThat(s.getAccessTokenHash()).isEqualTo(TokenGenerator.hash(access)).isNotEqualTo(access);
    assertThat(s.getRefreshTokenHash())
        .isEqualTo(TokenGenerator.hash(refresh))
        .isNotEqualTo(refresh);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("mobile").value(mobile("tokens")));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + refresh))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void registrationCanIncludeOptionalFarmerData() throws Exception {
    var body = registration(mobile("complete"));
    body.put("village", "Rampur");
    body.put("district", "Neemuch");
    body.put("selectedCrops", List.of("Wheat"));
    postJson("/auth/register", body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("user.village").value("Rampur"))
        .andExpect(jsonPath("user.profileCompleted").value(true));
  }

  @Test
  void mobilePasswordLoginReturnsFullUserAndWrongCredentialsAreRejected() throws Exception {
    registered("login");
    JsonNode r = loggedIn("login");
    assertThat(r.get("accessToken").asText()).hasSize(43);
    assertThat(r.get("refreshToken").asText()).hasSize(43);
    assertThat(r.at("/user/mobile").asText()).isEqualTo(mobile("login"));
    for (String number : List.of(mobile("login"), mobile("unknown")))
      postJson("/auth/login", Map.of("mobile", number, "password", "wrong-password"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("code").value("INVALID_CREDENTIALS"));
    postJson("/auth/login", Map.of("userId", "old.user.id", "password", PASSWORD))
        .andExpect(status().isBadRequest());
  }

  @Test
  void weakAndOverlongUtf8PasswordsAreRejected() throws Exception {
    for (String password : List.of("short", "क".repeat(30))) {
      var body = registration(mobile("weak"));
      body.put("password", password);
      body.put("confirmPassword", password);
      postJson("/auth/register", body).andExpect(status().isBadRequest());
    }
    assertThat(users.count()).isZero();
  }

  @Test
  void expiredAccessRefreshesToNewPairAndInvalidatesOldAccess() throws Exception {
    JsonNode first = registered("refresh");
    String access = first.get("accessToken").asText();
    AuthSession s = sessions.findByAccessTokenHash(TokenGenerator.hash(access)).orElseThrow();
    s.setAccessExpiresAt(Instant.now().minusSeconds(1));
    sessions.saveAndFlush(s);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isUnauthorized());
    JsonNode next =
        json.readTree(
            refresh(first.get("refreshToken").asText())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(next.get("accessToken").asText()).isNotEqualTo(access);
    assertThat(next.get("refreshToken")).isNotEqualTo(first.get("refreshToken"));
    assertThat(next.get("refreshTokenExpiresAt")).isEqualTo(first.get("refreshTokenExpiresAt"));
    assertThat(next.at("/user/mobile").asText()).isEqualTo(mobile("refresh"));
    mvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer " + next.get("accessToken").asText()))
        .andExpect(status().isOk());
  }

  @Test
  void rotatedRefreshReplayRevokesTheWholeFamily() throws Exception {
    JsonNode first = registered("replay");
    JsonNode next =
        json.readTree(
            refresh(first.get("refreshToken").asText())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    refresh(first.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer " + next.get("accessToken").asText()))
        .andExpect(status().isUnauthorized());
    refresh(next.get("refreshToken").asText()).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutAfterAccessExpiryRevokesOnlyItsLoginFamily() throws Exception {
    JsonNode first = registered("logout");
    JsonNode other = loggedIn("logout");
    AuthSession s =
        sessions
            .findByAccessTokenHash(TokenGenerator.hash(first.get("accessToken").asText()))
            .orElseThrow();
    s.setAccessExpiresAt(Instant.now().minusSeconds(1));
    sessions.saveAndFlush(s);
    postJson("/auth/logout", Map.of("refreshToken", first.get("refreshToken").asText()))
        .andExpect(status().isNoContent());
    refresh(first.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer " + other.get("accessToken").asText()))
        .andExpect(status().isOk());
  }

  @Test
  void expiredRefreshAndWrongTokenTypeAreRejected() throws Exception {
    JsonNode first = registered("expired");
    refresh(first.get("accessToken").asText()).andExpect(status().isUnauthorized());
    AuthSession s =
        sessions
            .findByAccessTokenHash(TokenGenerator.hash(first.get("accessToken").asText()))
            .orElseThrow();
    s.setRefreshExpiresAt(Instant.now().minusSeconds(1));
    sessions.saveAndFlush(s);
    refresh(first.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    refresh("fabricated").andExpect(status().isBadRequest());
  }

  @Test
  void profileIsIsolatedAndCannotSilentlyChangeLoginMobile() throws Exception {
    JsonNode first = registered("first");
    JsonNode second = registered("second");
    var profile =
        new HashMap<String, Object>(
            Map.of(
                "name",
                "First Farmer",
                "phone",
                mobile("first"),
                "village",
                "Test Village",
                "district",
                "Neemuch",
                "state",
                "MP",
                "language",
                "English",
                "landArea",
                "2 acres",
                "selectedCrops",
                List.of("Wheat")));
    mvc.perform(
            put("/api/users/me/profile")
                .header("Authorization", "Bearer " + first.get("accessToken").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(profile)))
        .andExpect(status().isOk());
    assertThat(loggedIn("first").at("/user/village").asText()).isEqualTo("Test Village");
    mvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer " + second.get("accessToken").asText()))
        .andExpect(jsonPath("village").value(""));
    profile.put("phone", mobile("changed"));
    mvc.perform(
            put("/api/users/me/profile")
                .header("Authorization", "Bearer " + first.get("accessToken").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(profile)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("MOBILE_CHANGE_NOT_SUPPORTED"));
  }

  FarmerUser legacy(String id, String number) {
    FarmerUser user = new FarmerUser();
    user.setUserId(id);
    user.setName("Legacy Farmer");
    user.setPhone(number);
    user.setPasswordHash(passwords.encode(PASSWORD));
    return users.saveAndFlush(user);
  }

  @Test
  void uniqueLegacyContactPhoneLinksOnlyAfterCorrectPasswordAndKeepsIdentity() throws Exception {
    FarmerUser old = legacy("legacy.user", "9876543210");
    postJson("/auth/login", Map.of("mobile", "9876543210", "password", "wrong-password"))
        .andExpect(status().isUnauthorized());
    assertThat(users.findById(old.getId()).orElseThrow().getMobileNumber()).isNull();
    postJson("/auth/login", Map.of("mobile", "9876543210", "password", PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("user.id").value(old.getId().toString()))
        .andExpect(jsonPath("user.mobile").value("9876543210"));
    assertThat(users.findById(old.getId()).orElseThrow().getMobileNumber()).isEqualTo("9876543210");
  }

  @Test
  void ambiguousLegacyPhoneIsNotAssignedToAnArbitraryAccount() throws Exception {
    legacy("legacy.one", "9876543210");
    legacy("legacy.two", "9876543210");
    postJson("/auth/login", Map.of("mobile", "9876543210", "password", PASSWORD))
        .andExpect(status().isUnauthorized());
    postJson("/auth/register", registration("9876543210")).andExpect(status().isConflict());
    assertThat(users.findByMobileNumber("9876543210")).isEmpty();
  }

  @Test
  void invalidPayloadAndProtectedRoutesReturnStructuredErrors() throws Exception {
    postJson("/auth/register", Map.of())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("VALIDATION_ERROR"));
    mvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("invalid-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("INVALID_REQUEST"));
    mvc.perform(put("/api/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void corsAllowsConfiguredOriginAndRejectsUnknownOrigin() throws Exception {
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", "http://localhost:8765")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8765"));
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", "https://unknown.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }
}
