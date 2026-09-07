package com.karim.gdmr_backend.staff.adapter.in.web;

import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.auth.domain.port.in.GetCurrentUserUseCase;
import com.karim.gdmr_backend.staff.adapter.in.web.dto.StaffProfileResponse;
import com.karim.gdmr_backend.staff.adapter.in.web.dto.UpdateStaffProfileRequest;
import com.karim.gdmr_backend.staff.domain.model.StaffProfile;
import com.karim.gdmr_backend.staff.domain.port.in.GetMyStaffProfileUseCase;
import com.karim.gdmr_backend.staff.domain.port.in.UpdateMyStaffProfileUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/profile")
@Tag(name = "Staff Profile", description = "HR/Doctor staff profile management")
public class StaffProfileController {

    private final GetMyStaffProfileUseCase getMyStaffProfileUseCase;
    private final UpdateMyStaffProfileUseCase updateMyStaffProfileUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public StaffProfileController(GetMyStaffProfileUseCase getMyStaffProfileUseCase,
                                   UpdateMyStaffProfileUseCase updateMyStaffProfileUseCase,
                                   GetCurrentUserUseCase getCurrentUserUseCase) {
        this.getMyStaffProfileUseCase = getMyStaffProfileUseCase;
        this.updateMyStaffProfileUseCase = updateMyStaffProfileUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @GetMapping
    public ResponseEntity<StaffProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        StaffProfile profile = getMyStaffProfileUseCase.getMyProfile(principal.userId());
        User user = getCurrentUserUseCase.getCurrentUser(principal.userId());
        return ResponseEntity.ok(StaffProfileResponse.from(user, profile));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<StaffProfileResponse> getProfile(@PathVariable Long userId) {
        StaffProfile profile = getMyStaffProfileUseCase.getMyProfile(userId);
        User user = getCurrentUserUseCase.getCurrentUser(userId);
        return ResponseEntity.ok(StaffProfileResponse.from(user, profile));
    }

    @PutMapping
    public ResponseEntity<StaffProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateStaffProfileRequest request) {

        StaffProfile profile = updateMyStaffProfileUseCase.updateMyProfile(
                new UpdateMyStaffProfileUseCase.UpdateStaffProfileCommand(
                        principal.userId(), request.phoneNumber(), request.jobTitle(),
                        request.hireDate(), request.officeLocation()));
        User user = getCurrentUserUseCase.getCurrentUser(principal.userId());
        return ResponseEntity.ok(StaffProfileResponse.from(user, profile));
    }

    // Admin editing another HR/Admin user's staff profile (e.g. from the Users
    // table's Edit action, or after resolving a profile-correction reclamation).
    @PutMapping("/{userId}")
    public ResponseEntity<StaffProfileResponse> updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateStaffProfileRequest request) {

        StaffProfile profile = updateMyStaffProfileUseCase.updateMyProfile(
                new UpdateMyStaffProfileUseCase.UpdateStaffProfileCommand(
                        userId, request.phoneNumber(), request.jobTitle(),
                        request.hireDate(), request.officeLocation()));
        User user = getCurrentUserUseCase.getCurrentUser(userId);
        return ResponseEntity.ok(StaffProfileResponse.from(user, profile));
    }
}
