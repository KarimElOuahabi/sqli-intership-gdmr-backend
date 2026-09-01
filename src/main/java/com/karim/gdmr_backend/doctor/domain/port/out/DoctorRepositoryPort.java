package com.karim.gdmr_backend.doctor.domain.port.out;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.doctor.domain.model.Doctor;
import com.karim.gdmr_backend.doctor.domain.port.in.ListDoctorsUseCase;

import java.util.Optional;

public interface DoctorRepositoryPort {
    Doctor save(Doctor doctor);
    Optional<Doctor> findById(Long doctorId);
    Optional<Doctor> findByUserId(Long userId);
    PageResult<Doctor> findAllPaged(ListDoctorsUseCase.ListDoctorsQuery query);
    void deleteByUserId(Long userId);
}
