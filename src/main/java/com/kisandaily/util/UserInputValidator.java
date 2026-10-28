package com.kisandaily.util;

import com.kisandaily.exceptions.ApiException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;

public final class UserInputValidator {
  private UserInputValidator() {}

  public static String mobile(String value) {
    if (value == null) return "";
    String number = value.strip().replaceAll("[\\s()-]", "");
    if (number.startsWith("+91")) number = number.substring(3);
    else if (number.length() == 12 && number.startsWith("91")) number = number.substring(2);
    else if (number.startsWith("0091")) number = number.substring(4);
    return number;
  }

  public static String name(String value) {
    String name = value.strip();
    if (name.length() < 2)
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_NAME", "नाम कम से कम 2 अक्षर का रखें");
    return name;
  }

  public static void password(String value) {
    if (value.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "Password UTF-8 में 72 bytes से छोटा रखें");
  }

  public static String text(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.strip();
  }
}
