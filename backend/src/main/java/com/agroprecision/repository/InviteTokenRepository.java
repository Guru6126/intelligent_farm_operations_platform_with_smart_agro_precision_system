package com.agroprecision.repository;

import com.agroprecision.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InviteTokenRepository extends JpaRepository<InviteToken, String> {
    Optional<InviteToken> findByToken(String token);
}

