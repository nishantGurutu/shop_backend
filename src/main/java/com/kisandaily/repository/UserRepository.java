package com.kisandaily.repository;

import com.kisandaily.entity.FarmerUser;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<FarmerUser, UUID> {
  Optional<FarmerUser> findByMobileNumber(String mobile);

  boolean existsByMobileNumber(String mobile);

  List<FarmerUser> findByPhoneInAndMobileNumberIsNull(Collection<String> phones);
}
