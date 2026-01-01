package com.authutil.authmaster.service;

import com.authutil.authmaster.dto.AuthResponse;
import com.authutil.authmaster.model.RefreshToken;
import com.authutil.authmaster.model.Role;
import com.authutil.authmaster.model.User;
import com.authutil.authmaster.repository.RefreshTokenRepository;
import com.authutil.authmaster.repository.UserRepository;
import com.authutil.authmaster.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       AuthenticationManager authManager) {
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authManager = authManager;
    }
    // ----------------------------------------
    // REGISTER
    // ----------------------------------------
    public User register(String username, String email, String rawPassword) {
        if (userRepo.existsByUsername(username)) throw new RuntimeException("Username exists");
        if (userRepo.existsByEmail(email)) throw new RuntimeException("Email exists");
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.getRoles().add(Role.ROLE_USER);
        user.setEnabled(true); // set false for email verification flows
        return userRepo.save(user);
    }
    // ----------------------------------------
    // LOGIN
    // ----------------------------------------
    public AuthResponse login(String username, String password) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        var principal = (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        User user = userRepo.findByUsername(username).orElseThrow();
        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getId());

        String refreshToken = createAndStoreRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, jwtUtils.getUsernameFromToken(accessToken));
    }
    // ----------------------------------------
    // HASH FUNCTION
    // ----------------------------------------
    private String hash(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
    // ----------------------------------------
    // Create Refresh Token
    // ----------------------------------------
    public String createAndStoreRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        String hashed = hash(rawToken);

        RefreshToken rt = new RefreshToken();
        rt.setHashToken(hashed);
        rt.setUser(user);
        // ROTATE token (invalidate old token)
        rt.setRevoked(false);
        rt.setExpiryDate(Instant.now().plusMillis(
                Long.parseLong(System.getProperty("app.jwt.refresh-expiration-ms",
                        String.valueOf(1209600000L)))));

        refreshRepo.save(rt);

        return rawToken;
    }

    // ----------------------------------------
    // ROTATE TOKEN + ISSUE NEW ACCESS TOKEN
    // ----------------------------------------
    public AuthResponse refreshAccessToken(String rawRefreshToken) {

        String hashed = hash(rawRefreshToken);
        RefreshToken stored = refreshRepo.findByHashToken(hashed)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (stored.getExpiryDate().isBefore(Instant.now())) {
            refreshRepo.delete(stored);
            throw new RuntimeException("Refresh token expired");
        }

        User user = stored.getUser();

        // ROTATE token (invalidate old token)
        stored.setRevoked(true);
        refreshRepo.save(stored);

        String newRawRefreshToken = createAndStoreRefreshToken(user);
        String newAccessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getId());

        return new AuthResponse(newAccessToken, newRawRefreshToken, user.getUsername());
    }

    // ----------------------------------------
    // LOGOUT
    // ----------------------------------------
    public void logout(String rawRefreshToken) {
        String hashed = hash(rawRefreshToken);

        refreshRepo.findByHashToken(hashed).ifPresent(token -> {
            token.setRevoked(true);
            refreshRepo.save(token);
        });
    }
}