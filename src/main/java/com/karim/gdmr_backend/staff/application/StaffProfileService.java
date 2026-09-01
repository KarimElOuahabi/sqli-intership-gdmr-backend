package com.karim.gdmr_backend.staff.application;

import com.karim.gdmr_backend.staff.domain.model.StaffProfile;
import com.karim.gdmr_backend.staff.domain.port.in.DeleteStaffProfileByUserIdUseCase;
import com.karim.gdmr_backend.staff.domain.port.in.GetMyStaffProfileUseCase;
import com.karim.gdmr_backend.staff.domain.port.in.UpdateMyStaffProfileUseCase;
import com.karim.gdmr_backend.staff.domain.port.out.StaffProfileRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StaffProfileService implements GetMyStaffProfileUseCase, UpdateMyStaffProfileUseCase, DeleteStaffProfileByUserIdUseCase {

    private final StaffProfileRepositoryPort staffProfileRepository;

    public StaffProfileService(StaffProfileRepositoryPort staffProfileRepository) {
        this.staffProfileRepository = staffProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffProfile getMyProfile(Long userId) {
        return staffProfileRepository.findByUserId(userId)
                .orElseGet(() -> StaffProfile.empty(userId));
    }

    @Override
    public StaffProfile updateMyProfile(UpdateStaffProfileCommand command) {
        StaffProfile existing = staffProfileRepository.findByUserId(command.userId()).orElse(null);

        StaffProfile toSave = new StaffProfile(
                existing != null ? existing.getId() : null,
                command.userId(),
                command.phoneNumber(),
                command.jobTitle(),
                command.hireDate(),
                command.officeLocation()
        );

        return staffProfileRepository.save(toSave);
    }

    @Override
    public void deleteByUserId(Long userId) {
        staffProfileRepository.deleteByUserId(userId);
    }
}
