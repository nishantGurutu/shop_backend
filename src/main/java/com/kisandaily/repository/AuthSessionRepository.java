package com.kisandaily.repository;

import com.kisandaily.entity.AuthSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
  Optional<AuthSession> findByAccessTokenHash(String hash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from AuthSession s where s.refreshTokenHash = :hash")
  Optional<AuthSession> findRefreshForUpdate(@Param("hash") String hash);

  @Modifying(flushAutomatically = true)
  @Query(
      "update AuthSession s set s.revokedAt=:now where s.familyId=:family and s.revokedAt is null")
  int revokeFamily(@Param("family") UUID family, @Param("now") Instant now);

  long deleteByRefreshExpiresAtBefore(Instant now);
}
