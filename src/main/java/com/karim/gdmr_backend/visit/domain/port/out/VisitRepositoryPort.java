package com.karim.gdmr_backend.visit.domain.port.out;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.visit.domain.model.Visit;
import com.karim.gdmr_backend.visit.domain.port.in.ListVisitsUseCase.ListVisitsQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitRepositoryPort {
    Visit save(Visit visit);
    Optional<Visit> findById(Long id);
    PageResult<Visit> findAllPaged(ListVisitsQuery query);
    List<Visit> findScheduledBetween(LocalDateTime from, LocalDateTime to);
    List<Long> findDistinctEmployeeIdsByDoctorId(Long doctorId);
}