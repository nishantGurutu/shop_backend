package com.kisandaily.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "farmer_users",
    uniqueConstraints = @UniqueConstraint(name = "uk_farmer_user_id", columnNames = "user_id"))
public class FarmerUser {
  @Id private java.util.UUID id = java.util.UUID.randomUUID();

  @Column(name = "user_id", nullable = false, length = 30)
  private String userId;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(length = 16)
  private String phone = "";

  @Column(name = "mobile_number", unique = true, length = 10)
  private String mobileNumber;

  @Column(length = 254)
  private String email;

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String value) {
    mobileNumber = value;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String value) {
    email = value;
  }

  @Column(length = 100)
  private String village = "";

  @Column(length = 100)
  private String district = "";

  @Column(length = 100)
  private String state = "मध्य प्रदेश";

  @Column(length = 40)
  private String language = "हिंदी (Hindi)";

  @Column(name = "land_area", length = 40)
  private String landArea = "";

  @Column(name = "profile_completed", nullable = false)
  private boolean profileCompleted = false;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "farmer_crops", joinColumns = @JoinColumn(name = "farmer_id"))
  @Column(name = "crop", length = 80)
  private java.util.List<String> selectedCrops = new java.util.ArrayList<>();

  public FarmerUser() {}

  public java.util.UUID getId() {
    return id;
  }

  public void setId(java.util.UUID value) {
    this.id = value;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String value) {
    this.userId = value;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String value) {
    this.passwordHash = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String value) {
    this.phone = value;
  }

  public String getVillage() {
    return village;
  }

  public void setVillage(String value) {
    this.village = value;
  }

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String value) {
    this.district = value;
  }

  public String getState() {
    return state;
  }

  public void setState(String value) {
    this.state = value;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String value) {
    this.language = value;
  }

  public String getLandArea() {
    return landArea;
  }

  public void setLandArea(String value) {
    this.landArea = value;
  }

  public boolean isProfileCompleted() {
    return profileCompleted;
  }

  public void setProfileCompleted(boolean value) {
    this.profileCompleted = value;
  }

  public java.util.List<String> getSelectedCrops() {
    return selectedCrops;
  }

  public void setSelectedCrops(java.util.List<String> value) {
    this.selectedCrops = value;
  }
}
