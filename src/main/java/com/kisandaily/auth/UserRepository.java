package com.kisandaily.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface UserRepository extends JpaRepository<FarmerUser,UUID> {
    Optional<FarmerUser> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
