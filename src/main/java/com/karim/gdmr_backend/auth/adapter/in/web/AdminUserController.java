package com.karim.gdmr_backend.auth.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.*;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.auth.domain.port.in.*;
import com.karim.gdmr_backend.doctor.domain.port.in.DeleteDoctorByUserIdUseCase;
import com.karim.gdmr_backend.doctor.domain.port.in.UpsertDoctorUseCase;
import com.karim.gdmr_backend.notification.domain.port.in.DeleteUserNotificationsUseCase;
import com.karim.gdmr_backend.profileissue.domain.port.in.DeleteUserProfileIssuesUseCase;
import com.karim.gdmr_backend.staff.domain.port.in.DeleteStaffProfileByUserIdUseCase;
import com.karim.gdmr_backend.staff.domain.port.in.UpdateMyStaffProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.karim.gdmr_backend.employee.domain.port.in.DeleteEmployeeByUserIdUseCase;
import com.karim.gdmr_backend.employee.domain.port.in.UpsertEmployeeUseCase;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final CreateUserByAdminUseCase createUserByAdminUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;
    private final GetRolesStatsUseCase getRolesStatsUseCase;
    private final UpsertEmployeeUseCase upsertEmployeeUseCase;
    private final UpsertDoctorUseCase upsertDoctorUseCase;
    private final UpdateMyStaffProfileUseCase updateStaffProfileUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final DeleteEmployeeByUserIdUseCase deleteEmployeeByUserIdUseCase;
    private final DeleteDoctorByUserIdUseCase deleteDoctorByUserIdUseCase;
    private final DeleteStaffProfileByUserIdUseCase deleteStaffProfileByUserIdUseCase;
    private final DeleteUserNotificationsUseCase deleteUserNotificationsUseCase;
    private final DeleteUserProfileIssuesUseCase deleteUserProfileIssuesUseCase;

    public AdminUserController(CreateUserByAdminUseCase createUserByAdminUseCase,
                               ListUsersUseCase listUsersUseCase,
                               UpdateUserUseCase updateUserUseCase,
                               ChangeUserStatusUseCase changeUserStatusUseCase,
                               GetRolesStatsUseCase getRolesStatsUseCase,
                               UpsertEmployeeUseCase upsertEmployeeUseCase, UpsertDoctorUseCase upsertDoctorUseCase,
                               UpdateMyStaffProfileUseCase updateStaffProfileUseCase,
                               DeleteUserUseCase deleteUserUseCase,
                               DeleteEmployeeByUserIdUseCase deleteEmployeeByUserIdUseCase,
                               DeleteDoctorByUserIdUseCase deleteDoctorByUserIdUseCase,
                               DeleteStaffProfileByUserIdUseCase deleteStaffProfileByUserIdUseCase,
                               DeleteUserNotificationsUseCase deleteUserNotificationsUseCase,
                               DeleteUserProfileIssuesUseCase deleteUserProfileIssuesUseCase) {
        this.createUserByAdminUseCase = createUserByAdminUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changeUserStatusUseCase = changeUserStatusUseCase;
        this.getRolesStatsUseCase = getRolesStatsUseCase;
        this.upsertEmployeeUseCase = upsertEmployeeUseCase;
        this.upsertDoctorUseCase = upsertDoctorUseCase;
        this.updateStaffProfileUseCase = updateStaffProfileUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.deleteEmployeeByUserIdUseCase = deleteEmployeeByUserIdUseCase;
        this.deleteDoctorByUserIdUseCase = deleteDoctorByUserIdUseCase;
        this.deleteStaffProfileByUserIdUseCase = deleteStaffProfileByUserIdUseCase;
        this.deleteUserNotificationsUseCase = deleteUserNotificationsUseCase;
        this.deleteUserProfileIssuesUseCase = deleteUserProfileIssuesUseCase;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var result = createUserByAdminUseCase.createUser(
                new CreateUserByAdminUseCase.CreateUserCommand(
                        request.firstName(), request.lastName(), request.role(), request.cin()));

        if (request.role() == Role.EMPLOYEE && request.employeeInfo() != null) {
            upsertEmployeeUseCase.upsertEmployee(
                    new UpsertEmployeeUseCase.UpsertEmployeeCommand(
                            result.user().getId(),
                            request.employeeInfo().birthDate(),
                            request.employeeInfo().department(),
                            request.employeeInfo().phoneNumber(),
                            request.employeeInfo().jobTitle(),
                            request.employeeInfo().hireDate(),
                            request.employeeInfo().cnssNumber()
                    )
            );
        }

        if (request.role() == Role.DOCTOR && request.doctorInfo() != null) {
            upsertDoctorUseCase.upsertDoctor(
                    new UpsertDoctorUseCase.UpsertDoctorCommand(
                            result.user().getId(),
                            request.doctorInfo().phoneNumber(),
                            request.doctorInfo().specialty(),
                            request.doctorInfo().qualifications(),
                            request.doctorInfo().yearsOfExperience(),
                            request.doctorInfo().workSite(),
                            request.doctorInfo().cnssNumber()
                    )
            );
        }

        if (request.role() == Role.HR && request.staffInfo() != null) {
            updateStaffProfileUseCase.updateMyProfile(
                    new UpdateMyStaffProfileUseCase.UpdateStaffProfileCommand(
                            result.user().getId(),
                            request.staffInfo().phoneNumber(),
                            request.staffInfo().jobTitle(),
                            request.staffInfo().hireDate(),
                            request.staffInfo().officeLocation()
                    )
            );
        }

        var response = new CreateUserResponse(UserResponse.from(result.user()), result.temporaryPassword());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<User> result = listUsersUseCase.listUsers(
                new ListUsersUseCase.ListUsersQuery(search, role, active, page, size));

        PageResult<UserResponse> mapped = new PageResult<>(
                result.content().stream().map(UserResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());

        return ResponseEntity.ok(PagedResponse.from(mapped));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        User updated = updateUserUseCase.updateUser(
                new UpdateUserUseCase.UpdateUserCommand(id, request.firstName(), request.lastName(), request.role(), request.cin()));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody ChangeUserStatusRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        if (!request.active() && id.equals(principal.userId())) {
            throw new IllegalStateException("You cannot deactivate your own account");
        }
        changeUserStatusUseCase.changeUserStatus(
                new ChangeUserStatusUseCase.ChangeUserStatusCommand(id, request.active()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/stats")
    public ResponseEntity<List<RolesStatsResponse>> getRolesStats() {
        List<GetRolesStatsUseCase.RolesStats> stats = getRolesStatsUseCase.getRolesStats();
        return ResponseEntity.ok(stats.stream().map(RolesStatsResponse::from).toList());
    }

    // Hard delete — for a user created with the wrong role/details and no real history yet.
    // Deactivate (above) remains the normal way to disable an account that has actually been used.
    // Order matters: FK-dependent rows (employee/doctor/staff profile) must go before the user row
    // itself, which is why this orchestrates across modules here rather than in a single service call.
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        if (id.equals(principal.userId())) {
            throw new IllegalStateException("You cannot delete your own account");
        }

        deleteEmployeeByUserIdUseCase.deleteByUserId(id);
        deleteDoctorByUserIdUseCase.deleteByUserId(id);
        deleteStaffProfileByUserIdUseCase.deleteByUserId(id);
        deleteUserNotificationsUseCase.deleteForUser(id);
        deleteUserProfileIssuesUseCase.deleteForUser(id);
        deleteUserUseCase.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

}