package com.karim.gdmr_backend.doctor.adapter.out.persistence;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.doctor.domain.model.Doctor;
import com.karim.gdmr_backend.doctor.domain.model.Speciality;
import com.karim.gdmr_backend.doctor.domain.port.in.ListDoctorsUseCase.ListDoctorsQuery;
import com.karim.gdmr_backend.doctor.domain.port.out.DoctorRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class DoctorRepositoryAdapter implements DoctorRepositoryPort {

    private final DoctorJpaRepository doctorJpaRepository;
    private final SpecialityJpaRepository specialityJpaRepository;

    public DoctorRepositoryAdapter(DoctorJpaRepository doctorJpaRepository,
                                   SpecialityJpaRepository specialityJpaRepository) {
        this.doctorJpaRepository = doctorJpaRepository;
        this.specialityJpaRepository = specialityJpaRepository;
    }

    @Override
    public Doctor save(Doctor doctor) {
        SpecialityEntity specialityEntity = specialityJpaRepository.findByName(doctor.getSpecialty().name())
                .orElseThrow(() -> new IllegalStateException("Speciality not found: " + doctor.getSpecialty()));

        DoctorEntity entity = new DoctorEntity(
                doctor.getId(),
                doctor.getUserId(),
                doctor.getPhoneNumber(),
                specialityEntity,
                doctor.getQualifications(),
                doctor.getYearsOfExperience(),
                doctor.getWorkSite(),
                doctor.getCnssNumber(),
                LocalDateTime.now()
        );
        return toDomain(doctorJpaRepository.save(entity));
    }

    @Override
    public Optional<Doctor> findById(Long doctorId) {
        return doctorJpaRepository.findById(doctorId).map(this::toDomain);
    }

    @Override
    public Optional<Doctor> findByUserId(Long userId) {
        return doctorJpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        doctorJpaRepository.findByUserId(userId).ifPresent(doctorJpaRepository::delete);
    }

    @Override
    public PageResult<Doctor> findAllPaged(ListDoctorsQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = doctorJpaRepository.findAll(pageable);

        List<Doctor> content = page.getContent().stream().map(this::toDomain).toList();

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Doctor toDomain(DoctorEntity entity) {
        Speciality speciality = Speciality.valueOf(entity.getSpecialty().getName());
        return new Doctor(
                entity.getId(),
                entity.getUserId(),
                entity.getPhoneNumber(),
                speciality,
                entity.getQualifications(),
                entity.getYearsOfExperience(),
                entity.getWorkSite(),
                entity.getCnssNumber()
        );
    }
}
