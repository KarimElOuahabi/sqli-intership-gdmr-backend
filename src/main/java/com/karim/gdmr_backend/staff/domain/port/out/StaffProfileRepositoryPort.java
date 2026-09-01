package com.karim.gdmr_backend.staff.domain.port.out;

import com.karim.gdmr_backend.staff.domain.model.StaffProfile;

import java.util.Optional;

public interface StaffProfileRepositoryPort {
    StaffProfile save(StaffProfile profile);
    Optional<StaffProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
