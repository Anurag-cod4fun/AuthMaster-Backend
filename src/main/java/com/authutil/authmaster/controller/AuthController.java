package com.authutil.authmaster.controller;


import com.authutil.authmaster.dto.*;
import com.authutil.authmaster.service.AuthService;
import com.authutil.authmaster.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService s) { this.authService = s; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        var u = authService.register(req.username(), req.email(), req.password());
        return ResponseEntity.ok("Registered: " + u.getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req.username(), req.password());
        // Create cookie
        ResponseCookie cookie = CookieUtil.createRefreshCookie(
                resp.refreshToken(),
                1209600 // 14 days
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(resp.accessToken(), resp.username()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest req) {

        String refreshToken = CookieUtil.extractRefreshToken(req);
        var resp = authService.refreshAccessToken(refreshToken);

        // Rotate refresh token: send new cookie
        ResponseCookie cookie = CookieUtil.createRefreshCookie(
                resp.refreshToken(),
                1209600
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(resp.accessToken(), resp.username()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req) {
        String refreshToken = CookieUtil.extractRefreshToken(req);
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = CookieUtil.deleteRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out");
    }
}
