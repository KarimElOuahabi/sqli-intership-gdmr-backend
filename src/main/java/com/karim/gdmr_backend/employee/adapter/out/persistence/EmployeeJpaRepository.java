package com.karim.gdmr_backend.employee.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findById(Long employeeId);
    Optional<EmployeeEntity> findByUserId(Long userId);
    Page<EmployeeEntity> findAll(Pageable pageable);

    // Matches on first name, last name, first+last combined, CIN, and CNSS
    // number — so typing "john smith" (both names at once), a CIN, or a CNSS
    // number all find the right employee, not just a single-field prefix match.
    @Query(value = """
            SELECT e.* FROM employees e JOIN users u ON u.id = e.user_id
            WHERE LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :search, '%'))
            """,
            countQuery = """
            SELECT count(*) FROM employees e JOIN users u ON u.id = e.user_id
            WHERE LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :search, '%'))
            """,
            nativeQuery = true)
    Page<EmployeeEntity> search(@Param("search") String search, Pageable pageable);

    // Two independent, optional filters ANDed together: nameSearch matches
    // first/last name, idSearch matches CIN, CNSS number, or the employee/user
    // numeric id — so HR can narrow by name and id at once instead of one
    // combined free-text field.
    @Query(value = """
            SELECT e.* FROM employees e JOIN users u ON u.id = e.user_id
            WHERE (:nameSearch IS NULL
               OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
              AND (:idSearch IS NULL
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR CAST(e.id AS TEXT) LIKE CONCAT('%', :idSearch, '%')
               OR CAST(u.id AS TEXT) LIKE CONCAT('%', :idSearch, '%'))
            """,
            countQuery = """
            SELECT count(*) FROM employees e JOIN users u ON u.id = e.user_id
            WHERE (:nameSearch IS NULL
               OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
              AND (:idSearch IS NULL
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR CAST(e.id AS TEXT) LIKE CONCAT('%', :idSearch, '%')
               OR CAST(u.id AS TEXT) LIKE CONCAT('%', :idSearch, '%'))
            """,
            nativeQuery = true)
    Page<EmployeeEntity> searchByNameAndId(@Param("nameSearch") String nameSearch,
                                            @Param("idSearch") String idSearch,
                                            Pageable pageable);

    // Same optional name/id filters as searchByNameAndId, but additionally
    // restricted to a known set of employee ids (a doctor's own patients).
    @Query(value = """
            SELECT e.* FROM employees e JOIN users u ON u.id = e.user_id
            WHERE e.id IN (:employeeIds)
              AND (:nameSearch IS NULL
               OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
              AND (:idSearch IS NULL
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR CAST(e.id AS TEXT) LIKE CONCAT('%', :idSearch, '%')
               OR CAST(u.id AS TEXT) LIKE CONCAT('%', :idSearch, '%'))
            """,
            countQuery = """
            SELECT count(*) FROM employees e JOIN users u ON u.id = e.user_id
            WHERE e.id IN (:employeeIds)
              AND (:nameSearch IS NULL
               OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
              AND (:idSearch IS NULL
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR LOWER(e.cnss_number) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR CAST(e.id AS TEXT) LIKE CONCAT('%', :idSearch, '%')
               OR CAST(u.id AS TEXT) LIKE CONCAT('%', :idSearch, '%'))
            """,
            nativeQuery = true)
    Page<EmployeeEntity> searchByIdsNameAndId(@Param("employeeIds") List<Long> employeeIds,
                                               @Param("nameSearch") String nameSearch,
                                               @Param("idSearch") String idSearch,
                                               Pageable pageable);
}