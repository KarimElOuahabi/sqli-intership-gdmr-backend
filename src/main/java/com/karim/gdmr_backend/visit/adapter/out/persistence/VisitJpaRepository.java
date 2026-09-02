package com.karim.gdmr_backend.visit.adapter.out.persistence;

import com.karim.gdmr_backend.visit.domain.model.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitJpaRepository extends JpaRepository<VisitEntity, Long> {

    @Query("""
    SELECT DISTINCT v FROM VisitEntity v
    LEFT JOIN FETCH v.proposedSlotsByEmployee
    WHERE (:employeeId IS NULL OR v.employeeId = :employeeId)
    AND (:doctorId IS NULL OR v.doctorId = :doctorId)
    AND (:status IS NULL OR v.status = :status)
    """)
    Page<VisitEntity> search(@Param("employeeId") Long employeeId,
                             @Param("doctorId") Long doctorId,
                             @Param("status") VisitStatus status,
                             Pageable pageable);

    @Query("""
    SELECT v FROM VisitEntity v
    WHERE v.status = com.karim.gdmr_backend.visit.domain.model.VisitStatus.SCHEDULED
    AND v.confirmedDateTime BETWEEN :from AND :to
    """)
    List<VisitEntity> findScheduledBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT DISTINCT v.employeeId FROM VisitEntity v WHERE v.doctorId = :doctorId")
    List<Long> findDistinctEmployeeIdsByDoctorId(@Param("doctorId") Long doctorId);
}