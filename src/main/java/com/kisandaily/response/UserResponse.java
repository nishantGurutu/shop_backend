package com.kisandaily.response;

import com.kisandaily.entity.FarmerUser;
import java.util.*;

public record UserResponse(
    UUID id,
    String userId,
    String name,
    String email,
    String mobile,
    String phone,
    boolean phoneVerified,
    String village,
    String district,
    String state,
    String language,
    String landArea,
    List<String> selectedCrops,
    boolean profileCompleted) {
  public static UserResponse from(FarmerUser u) {
    return new UserResponse(
        u.getId(),
        u.getUserId(),
        u.getName(),
        u.getEmail(),
        u.getMobileNumber(),
        u.getPhone(),
        false,
        u.getVillage(),
        u.getDistrict(),
        u.getState(),
        u.getLanguage(),
        u.getLandArea(),
        List.copyOf(u.getSelectedCrops()),
        u.isProfileCompleted());
  }
}
