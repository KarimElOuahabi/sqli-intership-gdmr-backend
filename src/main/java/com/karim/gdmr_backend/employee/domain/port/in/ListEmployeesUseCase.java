package com.karim.gdmr_backend.employee.domain.port.in;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.employee.domain.model.Employee;

import java.util.List;

public interface ListEmployeesUseCase {
    PageResult<Employee> listEmployees(ListEmployeesQuery query);

    // Restricts the result set to a known set of employee ids (e.g. a doctor's
    // own patients, derived from their visit history) before applying the same
    // name/id search + paging as the unrestricted listing.
    PageResult<Employee> listEmployeesByIds(ListEmployeesByIdsQuery query);

    record ListEmployeesQuery(String search, String idSearch, int page, int size) {}

    record ListEmployeesByIdsQuery(List<Long> employeeIds, String search, String idSearch, int page, int size) {}
}