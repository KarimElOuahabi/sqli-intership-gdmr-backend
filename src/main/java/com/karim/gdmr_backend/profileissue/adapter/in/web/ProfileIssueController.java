package com.karim.gdmr_backend.profileissue.adapter.in.web;

import com.karim.gdmr_backend.auth.domain.model.UserPrincipal;
import com.karim.gdmr_backend.profileissue.adapter.in.web.dto.ProfileIssueResponse;
import com.karim.gdmr_backend.profileissue.adapter.in.web.dto.ReportProfileIssueRequest;
import com.karim.gdmr_backend.profileissue.application.ProfileIssueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile-issues")
@Tag(name = "Profile Issues", description = "Reporting and resolving profile data issues")
public class ProfileIssueController {

    private final ProfileIssueService profileIssueService;

    public ProfileIssueController(ProfileIssueService profileIssueService) {
        this.profileIssueService = profileIssueService;
    }

    @PostMapping
    public ResponseEntity<Void> reportIssue(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody ReportProfileIssueRequest request) {
        profileIssueService.reportIssue(principal.userId(), request.fieldName(),
                request.suggestedCorrection(), request.note());
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<ProfileIssueResponse>> listForUser(@RequestParam Long userId) {
        List<ProfileIssueResponse> issues = profileIssueService.listForUser(userId).stream()
                .map(ProfileIssueResponse::from)
                .toList();
        return ResponseEntity.ok(issues);
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ProfileIssueResponse> resolve(@PathVariable Long id,
                                                          @RequestBody Map<String, Boolean> body) {
        boolean resolved = body.getOrDefault("resolved", true);
        return ResponseEntity.ok(ProfileIssueResponse.from(profileIssueService.setResolved(id, resolved)));
    }
}
