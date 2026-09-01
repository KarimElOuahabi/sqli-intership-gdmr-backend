package com.karim.gdmr_backend.profileissue.adapter.out.persistence;

import com.karim.gdmr_backend.profileissue.domain.model.ProfileIssue;
import com.karim.gdmr_backend.profileissue.domain.port.out.ProfileIssueRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProfileIssueRepositoryAdapter implements ProfileIssueRepositoryPort {

    private final ProfileIssueJpaRepository jpaRepository;

    public ProfileIssueRepositoryAdapter(ProfileIssueJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProfileIssue save(ProfileIssue issue) {
        ProfileIssueEntity entity = new ProfileIssueEntity(
                issue.getId(),
                issue.getReportedUserId(),
                issue.getReporterUserId(),
                issue.getFieldName(),
                issue.getSuggestedCorrection(),
                issue.getNote(),
                issue.isResolved(),
                issue.getCreatedAt() != null ? issue.getCreatedAt() : java.time.LocalDateTime.now()
        );
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<ProfileIssue> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProfileIssue> findAllByReportedUserId(Long reportedUserId) {
        return jpaRepository.findAllByReportedUserIdOrderByCreatedAtDesc(reportedUserId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void deleteAllForUser(Long userId) {
        jpaRepository.deleteAllByReportedUserIdOrReporterUserId(userId, userId);
    }

    private ProfileIssue toDomain(ProfileIssueEntity entity) {
        return new ProfileIssue(entity.getId(), entity.getReportedUserId(), entity.getReporterUserId(),
                entity.getFieldName(), entity.getSuggestedCorrection(), entity.getNote(),
                entity.isResolved(), entity.getCreatedAt());
    }
}
