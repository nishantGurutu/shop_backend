package com.kisandaily.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
  @Id private java.util.UUID id;

  @Column(name = "family_id", nullable = false)
  private java.util.UUID familyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_id", nullable = false)
  private FarmerUser user;

  @Column(name = "access_token_hash", nullable = false, unique = true, length = 64)
  private String accessTokenHash;

  @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
  private String refreshTokenHash;

  @Column(name = "access_expires_at", nullable = false)
  private java.time.Instant accessExpiresAt;

  @Column(name = "refresh_expires_at", nullable = false)
  private java.time.Instant refreshExpiresAt;

  @Column(name = "created_at", nullable = false)
  private java.time.Instant createdAt;

  @Column(name = "revoked_at")
  private java.time.Instant revokedAt;

  @Column(name = "replaced_by")
  private java.util.UUID replacedBy;

  public AuthSession() {}

  public java.util.UUID getId() {
    return id;
  }

  public void setId(java.util.UUID value) {
    this.id = value;
  }

  public java.util.UUID getFamilyId() {
    return familyId;
  }

  public void setFamilyId(java.util.UUID value) {
    this.familyId = value;
  }

  public FarmerUser getUser() {
    return user;
  }

  public void setUser(FarmerUser value) {
    this.user = value;
  }

  public String getAccessTokenHash() {
    return accessTokenHash;
  }

  public void setAccessTokenHash(String value) {
    this.accessTokenHash = value;
  }

  public String getRefreshTokenHash() {
    return refreshTokenHash;
  }

  public void setRefreshTokenHash(String value) {
    this.refreshTokenHash = value;
  }

  public java.time.Instant getAccessExpiresAt() {
    return accessExpiresAt;
  }

  public void setAccessExpiresAt(java.time.Instant value) {
    this.accessExpiresAt = value;
  }

  public java.time.Instant getRefreshExpiresAt() {
    return refreshExpiresAt;
  }

  public void setRefreshExpiresAt(java.time.Instant value) {
    this.refreshExpiresAt = value;
  }

  public java.time.Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(java.time.Instant value) {
    this.createdAt = value;
  }

  public java.time.Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(java.time.Instant value) {
    this.revokedAt = value;
  }

  public java.util.UUID getReplacedBy() {
    return replacedBy;
  }

  public void setReplacedBy(java.util.UUID value) {
    this.replacedBy = value;
  }
}
