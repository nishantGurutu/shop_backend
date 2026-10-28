package com.kisandaily.util;

import com.kisandaily.domain.TokenPair;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class TokenGenerator {
  private final SecureRandom random = new SecureRandom();

  private String generate() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public TokenPair pair() {
    return new TokenPair(generate(), generate());
  }

  public static boolean valid(String token) {
    return token != null && token.matches("[A-Za-z0-9_-]{43}");
  }

  public static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
