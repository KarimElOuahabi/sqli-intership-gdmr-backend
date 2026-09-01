package com.karim.gdmr_backend.auth.domain.port.out;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.in.GetRolesStatsUseCase;
import com.karim.gdmr_backend.auth.domain.port.in.ListUsersUseCase.ListUsersQuery;


import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    List<User> findAllByIds(List<Long> ids);
    boolean existsByEmail(String email);                    // new — for email collision checking
    PageResult<User> findAllPaged(ListUsersQuery query);      // new — for the admin datatable
    GetRolesStatsUseCase.RolesStats getRoleStats(Role role);
    void deleteById(Long id);
}
