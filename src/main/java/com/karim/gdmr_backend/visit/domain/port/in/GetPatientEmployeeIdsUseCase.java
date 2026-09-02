package com.karim.gdmr_backend.visit.domain.port.in;

import java.util.List;

public interface GetPatientEmployeeIdsUseCase {
    List<Long> getPatientEmployeeIds(Long doctorId);
}
