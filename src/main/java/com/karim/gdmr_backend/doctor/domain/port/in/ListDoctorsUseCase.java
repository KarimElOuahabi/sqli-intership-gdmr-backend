package com.karim.gdmr_backend.doctor.domain.port.in;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.doctor.domain.model.Doctor;

public interface ListDoctorsUseCase {
    PageResult<Doctor> listDoctors(ListDoctorsQuery query);

    record ListDoctorsQuery(String search, String idSearch, int page, int size) {}
}