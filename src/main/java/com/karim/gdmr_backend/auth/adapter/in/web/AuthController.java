package com.karim.gdmr_backend.auth.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.*;
import com.karim.gdmr_backend.auth.domain.exception.UserNotFoundException;
import com.karim.gdmr_backend.auth.domain.model.AuthTokens;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.auth.domain.port.in.*;
import com.karim.gdmr_backend.auth.domain.port.out.UserRepositoryPort;
import com.karim.gdmr_backend.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Login, registration, token refresh, logout, current user")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    @Value("${app.cookie.secure}")
    private boolean secureCookie;

    // Frontend and backend live on different domains once deployed (e.g. Vercel + Render),
    // which makes this a cross-site request — a SameSite=Strict cookie is never sent in that
    // case. "None" is only valid (browsers require it) when Secure is also true, so this stays
    // tied to the same flag that already distinguishes local dev from a real deployment.
    private String cookieSameSite() {
        return secureCookie ? "None" : "Strict";
    }

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          LoginUseCase loginUseCase,
                          RefreshAccessTokenUseCase refreshAccessTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        registerUserUseCase.register(request.email(), request.password(), request.firstName(), request.lastName(), request.role());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = loginUseCase.login(request.email(), request.password());
        return withRefreshCookie(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        AuthTokens tokens = refreshAccessTokenUseCase.refresh(refreshToken);
        return withRefreshCookie(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            logoutUseCase.logout(refreshToken);
        }

        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite())
                .path("/api/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.userId();

        // Fetch the user from the database using that ID
        User user = getCurrentUserUseCase.getCurrentUser(userId);

        return ResponseEntity.ok(UserResponse.from(user));
    }


    private ResponseEntity<AccessTokenResponse> withRefreshCookie(AuthTokens tokens) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.rawRefreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite())
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AccessTokenResponse(tokens.accessToken()));
    }
}