package com.karim.gdmr_backend.auth.adapter.in.web;

import com.karim.gdmr_backend.auth.adapter.in.web.dto.UserResponse;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.auth.domain.port.in.DeleteProfilePictureUseCase;
import com.karim.gdmr_backend.auth.domain.port.in.GetProfilePictureUseCase;
import com.karim.gdmr_backend.auth.domain.port.in.UpdateProfilePictureUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Auth", description = "Login, registration, token refresh, logout, current user")
public class ProfilePictureController {

    private final UpdateProfilePictureUseCase updateProfilePictureUseCase;
    private final GetProfilePictureUseCase getProfilePictureUseCase;
    private final DeleteProfilePictureUseCase deleteProfilePictureUseCase;

    public ProfilePictureController(UpdateProfilePictureUseCase updateProfilePictureUseCase,
                                     GetProfilePictureUseCase getProfilePictureUseCase,
                                     DeleteProfilePictureUseCase deleteProfilePictureUseCase) {
        this.updateProfilePictureUseCase = updateProfilePictureUseCase;
        this.getProfilePictureUseCase = getProfilePictureUseCase;
        this.deleteProfilePictureUseCase = deleteProfilePictureUseCase;
    }

    // Every role manages their own picture — there is no "on behalf of" upload.
    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> upload(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestParam("file") MultipartFile file) {
        User updated = updateProfilePictureUseCase.updateProfilePicture(
                new UpdateProfilePictureUseCase.UpdateProfilePictureCommand(principal.userId(), file));
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<UserResponse> delete(@AuthenticationPrincipal UserPrincipal principal) {
        User updated = deleteProfilePictureUseCase.deleteProfilePicture(principal.userId());
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    // Any authenticated user can view another user's picture (avatars in
    // tables, notifications, etc. are not sensitive) — 404 falls back to the
    // role-based default avatar on the frontend when there is none.
    @GetMapping("/{userId}/profile-picture")
    public ResponseEntity<byte[]> get(@PathVariable Long userId) {
        GetProfilePictureUseCase.ProfilePicture picture = getProfilePictureUseCase.getProfilePicture(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.contentType()))
                .header("Cache-Control", "private, max-age=300")
                .body(picture.data());
    }
}
