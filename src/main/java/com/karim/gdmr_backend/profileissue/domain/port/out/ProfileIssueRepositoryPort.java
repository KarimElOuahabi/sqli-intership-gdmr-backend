package com.karim.gdmr_backend.profileissue.domain.port.out;

import com.karim.gdmr_backend.profileissue.domain.model.ProfileIssue;

import java.util.List;
import java.util.Optional;

public interface ProfileIssueRepositoryPort {
    ProfileIssue save(ProfileIssue issue);
    Optional<ProfileIssue> findById(Long id);
    List<ProfileIssue> findAllByReportedUserId(Long reportedUserId);
    void deleteAllForUser(Long userId);
}
