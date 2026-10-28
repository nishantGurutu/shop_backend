package com.kisandaily.dto;

import com.kisandaily.util.UserInputValidator;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Locale;

public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 80) String name,
    @Email(message = "सही email लिखें") @Size(max = 254) String email,
    @NotBlank @Pattern(regexp = "[6-9][0-9]{9}", message = "सही 10 अंकों का मोबाइल नंबर लिखें")
        String mobile,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 72) String confirmPassword,
    @Size(max = 100) String village,
    @Size(max = 100) String district,
    @Size(max = 100) String state,
    @Size(max = 40) String language,
    @Size(max = 40) String landArea,
    @Size(max = 8) List<@NotBlank @Size(max = 80) String> selectedCrops) {
  public RegisterRequest {
    mobile = UserInputValidator.mobile(mobile);
    email = email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
  }
}
