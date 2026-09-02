package com.karim.gdmr_backend.visit.adapter.out.persistence;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.visit.domain.model.Visit;
import com.karim.gdmr_backend.visit.domain.port.in.ListVisitsUseCase.ListVisitsQuery;
import com.karim.gdmr_backend.visit.domain.port.out.VisitRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class VisitRepositoryAdapter implements VisitRepositoryPort {

    private final VisitJpaRepository jpaRepository;

    public VisitRepositoryAdapter(VisitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Visit save(Visit visit) {
        VisitEntity entity = new VisitEntity(
                visit.getId(),
                visit.getEmployeeId(),
                visit.getDoctorId(),
                visit.getVisitType(),
                visit.getStatus(),
                visit.getProposedSlotsByEmployee(),
                visit.getTimeSlotId(),
                visit.getConfirmedDateTime(),
                visit.getMotif(),
                visit.getReportNotes(),
                LocalDateTime.now()
        );
        VisitEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Visit> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Visit> findAllPaged(ListVisitsQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = jpaRepository.search(query.employeeId(), query.doctorId(), query.status(), pageable);

        List<Visit> content = page.getContent().stream().map(this::toDomain).toList();
        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public List<Visit> findScheduledBetween(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findScheduledBetween(from, to).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Long> findDistinctEmployeeIdsByDoctorId(Long doctorId) {
        return jpaRepository.findDistinctEmployeeIdsByDoctorId(doctorId);
    }

    private Visit toDomain(VisitEntity entity) {
        return new Visit(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getDoctorId(),
                entity.getVisitType(),
                entity.getStatus(),
                new ArrayList<>(entity.getProposedSlotsByEmployee()),  // forces init while session is open
                entity.getTimeSlotId(),
                entity.getConfirmedDateTime(),
                entity.getMotif(),
                entity.getReportNotes()
        );
    }
}