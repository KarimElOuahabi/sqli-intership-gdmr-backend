package com.karim.gdmr_backend.doctor.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoctorJpaRepository extends JpaRepository<DoctorEntity, Long> {
    Optional<DoctorEntity> findById(Long doctorId);
    Optional<DoctorEntity> findByUserId(Long userId);
    Page<DoctorEntity> findAll(Pageable pageable);

    // Two independent, optional filters ANDed together: nameSearch matches
    // first/last name, idSearch matches CIN, CNSS number, or the doctor/user
    // numeric id.
    @Query(value = """
            SELECT d.* FROM doctors d JOIN users u ON u.id = d.user_id
            WHERE (:nameSearch IS NULL
               OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
              AND (:idSearch IS NULL
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR LOWER(d.cnss_number) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR CAST(d.id AS TEXT) LIKE CONCAT('%', :idSearch, '%')
               OR CAST(u.id AS TEXT) LIKE CONCAT('%', :idSearch, '%'))
            """,
            countQuery = """
            SELECT count(*) FROM doctors d JOIN users u ON u.id = d.user_id
            WHERE (:nameSearch IS NULL
               OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :nameSearch, '%'))
               OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
              AND (:idSearch IS NULL
               OR LOWER(u.cin) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR LOWER(d.cnss_number) LIKE LOWER(CONCAT('%', :idSearch, '%'))
               OR CAST(d.id AS TEXT) LIKE CONCAT('%', :idSearch, '%')
               OR CAST(u.id AS TEXT) LIKE CONCAT('%', :idSearch, '%'))
            """,
            nativeQuery = true)
    Page<DoctorEntity> searchByNameAndId(@Param("nameSearch") String nameSearch,
                                          @Param("idSearch") String idSearch,
                                          Pageable pageable);
}
