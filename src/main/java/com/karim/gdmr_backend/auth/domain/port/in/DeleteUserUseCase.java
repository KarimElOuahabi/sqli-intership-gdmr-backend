package com.karim.gdmr_backend.auth.domain.port.in;

/** Hard-deletes a user row and their own auth-adjacent records (refresh tokens). Call after
 * the other modules' own delete-by-user-id use cases so FK-dependent rows (employee/doctor/
 * staff profile) are already gone. */
public interface DeleteUserUseCase {
    void deleteUser(Long userId);
}
