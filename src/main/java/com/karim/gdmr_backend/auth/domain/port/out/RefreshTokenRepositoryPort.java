package com.karim.gdmr_backend.auth.domain.port.out;

import com.karim.gdmr_backend.auth.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    void save(RefreshToken refreshToken);
    Optional<RefreshToken> findValidByHash(String tokenHash);
    void revokeByHash(String tokenHash);

    void revokeAllByUserId(Long userId);
    void deleteAllByUserId(Long userId);
}
