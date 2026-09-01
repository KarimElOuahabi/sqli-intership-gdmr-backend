package com.karim.gdmr_backend.profileissue.application;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.in.GetCurrentUserUseCase;
import com.karim.gdmr_backend.auth.domain.port.in.ListUsersUseCase;
import com.karim.gdmr_backend.notification.domain.model.NotificationType;
import com.karim.gdmr_backend.notification.domain.port.in.SendNotificationUseCase;
import com.karim.gdmr_backend.profileissue.domain.exception.ProfileIssueNotFoundException;
import com.karim.gdmr_backend.profileissue.domain.model.ProfileIssue;
import com.karim.gdmr_backend.profileissue.domain.port.in.DeleteUserProfileIssuesUseCase;
import com.karim.gdmr_backend.profileissue.domain.port.in.ListProfileIssuesUseCase;
import com.karim.gdmr_backend.profileissue.domain.port.in.ResolveProfileIssueUseCase;
import com.karim.gdmr_backend.profileissue.domain.port.out.ProfileIssueRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional
public class ProfileIssueService implements ListProfileIssuesUseCase, ResolveProfileIssueUseCase, DeleteUserProfileIssuesUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final ProfileIssueRepositoryPort profileIssueRepository;

    public ProfileIssueService(GetCurrentUserUseCase getCurrentUserUseCase,
                                ListUsersUseCase listUsersUseCase,
                                SendNotificationUseCase sendNotificationUseCase,
                                ProfileIssueRepositoryPort profileIssueRepository) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.profileIssueRepository = profileIssueRepository;
    }

    public void reportIssue(Long reportingUserId, String fieldName, String suggestedCorrection, String note) {
        User reporter = getCurrentUserUseCase.getCurrentUser(reportingUserId);

        profileIssueRepository.save(ProfileIssue.createNew(
                reportingUserId, reportingUserId, fieldName, suggestedCorrection, note));

        String title = "Profile correction requested";
        String message = String.format(
                "%s %s (%s) flagged their \"%s\" field as incorrect. Suggested correction: \"%s\".%s",
                reporter.getFirstName(), reporter.getLastName(), reporter.getRole(),
                fieldName, suggestedCorrection,
                (note != null && !note.isBlank()) ? " Note: " + note : ""
        );

        Stream.of(Role.ADMIN, Role.HR)
                .flatMap(role -> listUsersUseCase.listUsers(
                        new ListUsersUseCase.ListUsersQuery(null, role, true, 0, 500)).content().stream())
                .filter(recipient -> !recipient.getId().equals(reportingUserId))
                .forEach(recipient -> sendNotificationUseCase.send(
                        new SendNotificationUseCase.SendNotificationCommand(
                                recipient.getId(), NotificationType.PROFILE_ISSUE_REPORTED, title, message,
                                null, reportingUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileIssue> listForUser(Long reportedUserId) {
        return profileIssueRepository.findAllByReportedUserId(reportedUserId);
    }

    @Override
    public ProfileIssue setResolved(Long issueId, boolean resolved) {
        ProfileIssue existing = profileIssueRepository.findById(issueId)
                .orElseThrow(() -> new ProfileIssueNotFoundException(issueId));
        return profileIssueRepository.save(existing.withResolved(resolved));
    }

    @Override
    public void deleteForUser(Long userId) {
        profileIssueRepository.deleteAllForUser(userId);
    }
}
