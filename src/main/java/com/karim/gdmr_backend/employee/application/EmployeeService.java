package com.karim.gdmr_backend.employee.application;

import com.karim.gdmr_backend.auth.domain.exception.UserNotFoundException;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.out.UserRepositoryPort;
import com.karim.gdmr_backend.employee.domain.exception.EmployeeNotFoundException;
import com.karim.gdmr_backend.employee.domain.exception.InvalidRoleForEmployeeException;
import com.karim.gdmr_backend.employee.domain.model.Employee;
import com.karim.gdmr_backend.employee.domain.port.in.*;
import com.karim.gdmr_backend.employee.domain.port.out.EmployeeRepositoryPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class EmployeeService implements GetMyProfileUseCase, UpsertEmployeeUseCase, ListEmployeesUseCase, GetEmployeeByIdUseCase, GetEmployeeIdByUserIdUseCase, DeleteEmployeeByUserIdUseCase {

    private final EmployeeRepositoryPort employeeRepository;
    private final UserRepositoryPort userRepository;

    public EmployeeService(EmployeeRepositoryPort employeeRepository, UserRepositoryPort userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Employee getMyProfile(Long userId) {
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException(userId));

    }

    @Override
    public Employee upsertEmployee(UpsertEmployeeCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if(user.getRole() != Role.EMPLOYEE) {
            throw new InvalidRoleForEmployeeException(user.getRole());
        }

        Employee existing = employeeRepository.findByUserId(command.userId())
                .orElse(null);

        Employee toSave = existing == null
                ? Employee.createNew(null, command.userId(), command.birthDate(), command.department(),
                        command.phoneNumber(), command.jobTitle(), command.hireDate(), command.cnssNumber())
                : new Employee(existing.getId(), command.userId(), command.birthDate(), command.department(),
                        command.phoneNumber(), command.jobTitle(), command.hireDate(), command.cnssNumber());

        return employeeRepository.save(toSave);

    }

    @Override
    public PageResult<Employee> listEmployees(ListEmployeesQuery query) {
        return employeeRepository.findAllPaged(query);
    }

    @Override
    public PageResult<Employee> listEmployeesByIds(ListEmployeesByIdsQuery query) {
        return employeeRepository.findAllPagedByIds(query);
    }

    @Override
    public Employee getEmployeeById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
    }

    @Override
    public Long getEmployeeId(Long userId) {
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException(userId))
                .getId();
    }

    @Override
    public void deleteByUserId(Long userId) {
        employeeRepository.deleteByUserId(userId);
    }
}
