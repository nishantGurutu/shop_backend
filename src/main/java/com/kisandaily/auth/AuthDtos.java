package com.kisandaily.auth;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
class AuthDtos {
    record Register(@NotBlank @Size(max=64) String userId,
                    @NotBlank @Size(min=8,max=72) String password,
                    @NotBlank @Size(min=2,max=80) String name,
                    @Pattern(regexp="(?:[6-9][0-9]{9})?", message="सही 10 अंकों का मोबाइल नंबर लिखें") String phone) {}
    record Login(@NotBlank @Size(max=64) String userId, @NotBlank @Size(max=72) String password) {}
    record Profile(@NotBlank @Size(min=2,max=80) String name,
                   @Pattern(regexp="(?:[6-9][0-9]{9})?", message="सही मोबाइल नंबर लिखें") String phone,
                   @NotBlank @Size(max=100) String village, @NotBlank @Size(max=100) String district,
                   @NotBlank @Size(max=100) String state, @NotBlank @Size(max=40) String language,
                   @Size(max=40) String landArea,
                   @NotEmpty @Size(max=8) List<@NotBlank @Size(max=80) String> selectedCrops) {}
    record User(UUID id, String userId, String name, String phone, String village, String district,
                String state, String language, String landArea, List<String> selectedCrops, boolean profileCompleted) {
        static User from(FarmerUser u) { return new User(u.id,u.userId,u.name,u.phone,u.village,u.district,u.state,u.language,u.landArea,List.copyOf(u.selectedCrops),u.profileCompleted); }
    }
    record Session(String token, Instant expiresAt, User user) {}
    record Principal(UUID id, String userId) {}
    record Error(String code, String message, Map<String,String> fields) {}
}
