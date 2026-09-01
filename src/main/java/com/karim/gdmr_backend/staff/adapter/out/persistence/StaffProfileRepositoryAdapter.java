package com.karim.gdmr_backend.staff.adapter.out.persistence;

import com.karim.gdmr_backend.staff.domain.model.StaffProfile;
import com.karim.gdmr_backend.staff.domain.port.out.StaffProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class StaffProfileRepositoryAdapter implements StaffProfileRepositoryPort {

    private final StaffProfileJpaRepository jpaRepository;

    public StaffProfileRepositoryAdapter(StaffProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StaffProfile save(StaffProfile profile) {
        StaffProfileEntity entity = new StaffProfileEntity(
                profile.getId(),
                profile.getUserId(),
                profile.getPhoneNumber(),
                profile.getJobTitle(),
                profile.getHireDate(),
                profile.getOfficeLocation(),
                LocalDateTime.now()
        );
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<StaffProfile> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.findByUserId(userId).ifPresent(jpaRepository::delete);
    }

    private StaffProfile toDomain(StaffProfileEntity entity) {
        return new StaffProfile(
                entity.getId(),
                entity.getUserId(),
                entity.getPhoneNumber(),
                entity.getJobTitle(),
                entity.getHireDate(),
                entity.getOfficeLocation()
        );
    }
}
