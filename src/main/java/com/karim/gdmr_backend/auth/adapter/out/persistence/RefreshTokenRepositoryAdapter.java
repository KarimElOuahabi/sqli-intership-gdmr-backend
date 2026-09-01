package com.karim.gdmr_backend.auth.adapter.out.persistence;

import com.karim.gdmr_backend.auth.domain.model.RefreshToken;
import com.karim.gdmr_backend.auth.domain.port.out.RefreshTokenRepositoryPort;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Optional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity(
                refreshToken.getId(),
                refreshToken.getUserId(),
                refreshToken.getTokenHash(),
                refreshToken.getExpiresAt(),
                refreshToken.isRevoked(),
                Instant.now()
        );
        jpaRepository.save(entity);
    }

    @Override
    public Optional<RefreshToken> findValidByHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain)
                .filter(RefreshToken::isValid);
    }

    @Override
    public void revokeByHash(String tokenHash) {
        jpaRepository.findByTokenHash(tokenHash).ifPresent(entity -> {
            entity.setRevoked(true);
            jpaRepository.save(entity);
        });
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        jpaRepository.revokeAllByUserId(userId);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.isRevoked());
    }
}