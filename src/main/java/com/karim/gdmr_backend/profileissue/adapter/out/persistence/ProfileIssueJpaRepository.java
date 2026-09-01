package com.karim.gdmr_backend.profileissue.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileIssueJpaRepository extends JpaRepository<ProfileIssueEntity, Long> {
    List<ProfileIssueEntity> findAllByReportedUserIdOrderByCreatedAtDesc(Long reportedUserId);

    void deleteAllByReportedUserIdOrReporterUserId(Long reportedUserId, Long reporterUserId);
}
