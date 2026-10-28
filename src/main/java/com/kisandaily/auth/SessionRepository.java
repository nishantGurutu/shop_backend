package com.kisandaily.auth;
import org.springframework.data.jpa.repository.JpaRepository;
interface SessionRepository extends JpaRepository<LoginSession,String> {}
