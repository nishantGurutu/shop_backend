package com.kisandaily.dto;

import com.kisandaily.util.UserInputValidator;
import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Pattern(regexp = "[6-9][0-9]{9}", message = "सही मोबाइल नंबर लिखें") String mobile,
    @NotBlank @Size(max = 72) String password) {
  public LoginRequest {
    mobile = UserInputValidator.mobile(mobile);
  }
}
