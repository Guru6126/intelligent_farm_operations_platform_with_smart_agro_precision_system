package com.agroprecision.repository;

import com.agroprecision.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByResetToken(String token);
    boolean existsByEmailIgnoreCase(String email);
}
