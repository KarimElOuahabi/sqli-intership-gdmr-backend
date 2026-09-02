package com.karim.gdmr_backend.doctor.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.PagedResponse;
import com.karim.gdmr_backend.auth.adapter.in.web.dto.UserResponse;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.auth.domain.port.in.GetCurrentUserUseCase;
import com.karim.gdmr_backend.auth.domain.port.in.GetUsersByIdsUseCase;
import com.karim.gdmr_backend.doctor.adapter.in.web.dto.DoctorProfileResponse;
import com.karim.gdmr_backend.doctor.adapter.in.web.dto.DoctorResponse;
import com.karim.gdmr_backend.doctor.adapter.in.web.dto.UpsertDoctorRequest;
import com.karim.gdmr_backend.doctor.domain.model.Doctor;
import com.karim.gdmr_backend.doctor.domain.port.in.GetMyProfileUseCase;
import com.karim.gdmr_backend.doctor.domain.port.in.ListDoctorsUseCase;
import com.karim.gdmr_backend.doctor.domain.port.in.UpsertDoctorUseCase;
import com.karim.gdmr_backend.shared.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.karim.gdmr_backend.doctor.domain.port.in.GetDoctorByIdUseCase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DoctorController {

    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpsertDoctorUseCase upsertDoctorUseCase;
    private final ListDoctorsUseCase listDoctorsUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetUsersByIdsUseCase getUsersByIdsUseCase;
    private final GetDoctorByIdUseCase getDoctorByIdUseCase;

    public DoctorController(GetMyProfileUseCase getMyProfileUseCase,
                            UpsertDoctorUseCase upsertDoctorUseCase,
                            ListDoctorsUseCase listDoctorsUseCase,
                            GetCurrentUserUseCase getCurrentUserUseCase,
                            GetUsersByIdsUseCase getUsersByIdsUseCase,
                            GetDoctorByIdUseCase getDoctorByIdUseCase) {
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.upsertDoctorUseCase = upsertDoctorUseCase;
        this.listDoctorsUseCase = listDoctorsUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.getUsersByIdsUseCase = getUsersByIdsUseCase;
        this.getDoctorByIdUseCase = getDoctorByIdUseCase;
    }

    @GetMapping("/api/doctor/profile")
    public ResponseEntity<DoctorProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Doctor doctor = getMyProfileUseCase.getMyProfile(principal.userId());
        User user = getCurrentUserUseCase.getCurrentUser(principal.userId());
        return ResponseEntity.ok(DoctorProfileResponse.from(user, doctor));
    }

    @GetMapping("/api/doctor/profile/{doctorId}")
    public ResponseEntity<DoctorProfileResponse> getDoctorProfile(@PathVariable Long doctorId) {
        Doctor doctor = getDoctorByIdUseCase.getDoctorById(doctorId);
        User targetUser = getCurrentUserUseCase.getCurrentUser(doctor.getUserId());
        return ResponseEntity.ok(DoctorProfileResponse.from(targetUser, doctor));
    }

    @GetMapping("/api/doctor/profile/user/{userId}")
    public ResponseEntity<DoctorProfileResponse> getDoctorProfileByUserId(@PathVariable Long userId) {
        Doctor doctor = getMyProfileUseCase.getMyProfile(userId);
        User targetUser = getCurrentUserUseCase.getCurrentUser(userId);
        return ResponseEntity.ok(DoctorProfileResponse.from(targetUser, doctor));
    }

    @GetMapping("/api/doctors")
    public ResponseEntity<PagedResponse<DoctorProfileResponse>> listDoctors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String idSearch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<Doctor> result = listDoctorsUseCase.listDoctors(
                new ListDoctorsUseCase.ListDoctorsQuery(search, idSearch, page, size));

        List<Long> userIds = result.content().stream().map(Doctor::getUserId).toList();
        Map<Long, User> usersById = getUsersByIdsUseCase.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<DoctorProfileResponse> content = result.content().stream()
                .map(doctor -> DoctorProfileResponse.from(usersById.get(doctor.getUserId()), doctor))
                .toList();

        PageResult<DoctorProfileResponse> mapped = new PageResult<>(
                content, result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(PagedResponse.from(mapped));
    }

    @PutMapping("/api/doctor/{userId}")
    public ResponseEntity<DoctorResponse> upsertDoctor(
            @PathVariable Long userId,
            @RequestBody UpsertDoctorRequest request) {

        Doctor saved = upsertDoctorUseCase.upsertDoctor(
                new UpsertDoctorUseCase.UpsertDoctorCommand(
                        userId,
                        request.phoneNumber(),
                        request.specialty(),
                        request.qualifications(),
                        request.yearsOfExperience(),
                        request.workSite(),
                        request.cnssNumber()
                )
        );
        return ResponseEntity.ok(DoctorResponse.from(saved));
    }

    private Long extractUserId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}