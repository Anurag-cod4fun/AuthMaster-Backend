package com.authutil.authmaster.repository;

import com.authutil.authmaster.model.RefreshToken;
import com.authutil.authmaster.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByHashToken(String token);
    void deleteByUser(User user);
}