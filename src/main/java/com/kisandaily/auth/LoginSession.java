package com.kisandaily.auth;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="login_sessions", indexes=@Index(name="idx_session_expiry", columnList="expires_at"))
class LoginSession {
    @Id @Column(length=64) String tokenHash;
    @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="farmer_id") FarmerUser user;
    @Column(name="expires_at", nullable=false) Instant expiresAt;
    protected LoginSession() {}
    LoginSession(String tokenHash, FarmerUser user, Instant expiresAt) { this.tokenHash=tokenHash; this.user=user; this.expiresAt=expiresAt; }
}
