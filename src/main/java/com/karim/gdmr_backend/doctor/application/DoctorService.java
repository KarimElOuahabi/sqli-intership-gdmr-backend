package com.karim.gdmr_backend.doctor.application;

import com.karim.gdmr_backend.auth.domain.exception.UserNotFoundException;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.out.UserRepositoryPort;
import com.karim.gdmr_backend.doctor.domain.exception.DoctorNotFoundException;
import com.karim.gdmr_backend.doctor.domain.exception.InvalidRoleForDoctorException;
import com.karim.gdmr_backend.doctor.domain.model.Doctor;
import com.karim.gdmr_backend.doctor.domain.model.Speciality;
import com.karim.gdmr_backend.doctor.domain.port.in.*;
import com.karim.gdmr_backend.doctor.domain.port.out.DoctorRepositoryPort;
import com.karim.gdmr_backend.employee.domain.exception.EmployeeNotFoundException;
import com.karim.gdmr_backend.employee.domain.exception.InvalidRoleForEmployeeException;
import com.karim.gdmr_backend.employee.domain.model.Employee;
import com.karim.gdmr_backend.employee.domain.port.in.ListEmployeesUseCase;
import com.karim.gdmr_backend.employee.domain.port.in.UpsertEmployeeUseCase;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class DoctorService implements GetMyProfileUseCase, UpsertDoctorUseCase, ListDoctorsUseCase, GetDoctorByIdUseCase, GetDoctorIdByUserIdUseCase, DeleteDoctorByUserIdUseCase {

    private final DoctorRepositoryPort doctorRepository;
    private final UserRepositoryPort userRepository;

    public DoctorService(DoctorRepositoryPort doctorRepository, UserRepositoryPort userRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Doctor getMyProfile(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException(userId));

    }

    @Override
    public Doctor upsertDoctor(UpsertDoctorUseCase.UpsertDoctorCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if(user.getRole() != Role.DOCTOR) {
            throw new InvalidRoleForEmployeeException(user.getRole());
        }

        Doctor existing = doctorRepository.findByUserId(command.userId())
                .orElse(null);

        Doctor toSave = (existing == null)
                ? Doctor.createNew(null , command.userId(), command.phoneNumber(), command.specialty(), command.qualifications(), command.yearsOfExperience(), command.workSite(), command.cnssNumber())
                : new Doctor(existing.getId(), command.userId(), command.phoneNumber(), command.specialty(), command.qualifications(), command.yearsOfExperience(), command.workSite(), command.cnssNumber());

        return doctorRepository.save(toSave);

    }

    @Override
    public PageResult<Doctor> listDoctors(ListDoctorsUseCase.ListDoctorsQuery query) {
        return doctorRepository.findAllPaged(query);
    }

    @Override
    public Doctor getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));
    }

    @Override
    public Long getDoctorId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException(userId))
                .getId();
    }

    @Override
    public void deleteByUserId(Long userId) {
        doctorRepository.deleteByUserId(userId);
    }
}
