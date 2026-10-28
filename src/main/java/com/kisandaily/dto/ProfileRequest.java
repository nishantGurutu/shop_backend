package com.kisandaily.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record ProfileRequest(
    @NotBlank @Size(min = 2, max = 80) String name,
    @Pattern(regexp = "(?:[6-9][0-9]{9})?", message = "सही मोबाइल नंबर लिखें") String phone,
    @NotBlank @Size(max = 100) String village,
    @NotBlank @Size(max = 100) String district,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Size(max = 40) String language,
    @Size(max = 40) String landArea,
    @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 80) String> selectedCrops) {}
