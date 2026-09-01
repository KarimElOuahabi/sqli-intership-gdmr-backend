package com.karim.gdmr_backend.employee.adapter.out.persistence;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.employee.domain.model.Department;
import com.karim.gdmr_backend.employee.domain.model.Employee;
import com.karim.gdmr_backend.employee.domain.port.in.ListEmployeesUseCase.ListEmployeesQuery;
import com.karim.gdmr_backend.employee.domain.port.out.EmployeeRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class EmployeeRepositoryAdapter implements EmployeeRepositoryPort {

    private final EmployeeJpaRepository employeeJpaRepository;
    private final DepartmentJpaRepository departmentJpaRepository;

    public EmployeeRepositoryAdapter(EmployeeJpaRepository jpaRepository,
                                     DepartmentJpaRepository departmentJpaRepository) {
        this.employeeJpaRepository = jpaRepository;
        this.departmentJpaRepository = departmentJpaRepository;

    }

    @Override
    public Employee save(Employee employee) {
        DepartmentEntity departmentEntity = departmentJpaRepository.findByName(employee.getDepartment().name())
                .orElseThrow(() -> new IllegalStateException("Department not found: " + employee.getDepartment()));

        EmployeeEntity entity = new EmployeeEntity(
                employee.getId(),
                employee.getUserId(),
                employee.getBirthDate(),
                departmentEntity,
                employee.getPhoneNumber(),
                employee.getJobTitle(),
                employee.getHireDate(),
                employee.getCnssNumber(),
                LocalDateTime.now()
        );
        return toDomain(employeeJpaRepository.save(entity));
    }

    @Override
    public Optional<Employee> findById(Long employeeId) {
        return employeeJpaRepository.findById(employeeId).map(this::toDomain);
    }

    @Override
    public Optional<Employee> findByUserId(Long userId) {
        return employeeJpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        employeeJpaRepository.findByUserId(userId).ifPresent(employeeJpaRepository::delete);
    }

    @Override
    public PageResult<Employee> findAllPaged(ListEmployeesQuery query) {
        boolean hasSearch = query.search() != null && !query.search().isBlank();
        var pageable = hasSearch
                ? PageRequest.of(query.page(), query.size())
                : PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = hasSearch
                ? employeeJpaRepository.search(query.search(), pageable)
                : employeeJpaRepository.findAll(pageable);

        List<Employee> content = page.getContent().stream().map(this::toDomain).toList();

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Employee toDomain(EmployeeEntity entity) {
        Department department = Department.valueOf(entity.getDepartment().getName());
        return new Employee(
                entity.getId(),
                entity.getUserId(),
                entity.getBirthDate(),
                department,
                entity.getPhoneNumber(),
                entity.getJobTitle(),
                entity.getHireDate(),
                entity.getCnssNumber()
        );
    }
}