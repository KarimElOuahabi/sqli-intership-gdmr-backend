package com.karim.gdmr_backend.employee.domain.port.out;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.employee.domain.model.Employee;
import com.karim.gdmr_backend.employee.domain.port.in.ListEmployeesUseCase;

import java.util.Optional;

public interface EmployeeRepositoryPort {
    Employee save(Employee employee);
    Optional<Employee> findById(Long id);
    Optional<Employee> findByUserId(Long userId);
    PageResult<Employee> findAllPaged(ListEmployeesUseCase.ListEmployeesQuery query);
    void deleteByUserId(Long userId);
}