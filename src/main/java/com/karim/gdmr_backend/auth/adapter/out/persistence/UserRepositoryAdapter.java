package com.karim.gdmr_backend.auth.adapter.out.persistence;

import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.in.GetRolesStatsUseCase;
import com.karim.gdmr_backend.auth.domain.port.in.ListUsersUseCase.ListUsersQuery;
import com.karim.gdmr_backend.auth.domain.port.out.UserRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository,
                                 RoleJpaRepository roleJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
    }

    @Override
    public User save(User user) {
        RoleEntity roleEntity = roleJpaRepository.findByName(user.getRole().name())
                .orElseThrow(() -> new IllegalStateException("Role not found: " + user.getRole()));

        UserEntity entity = new UserEntity(
                user.getId(),
                user.getEmail(),
                user.getHashedPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getCin(),
                user.getProfilePictureKey(),
                user.getProfilePictureContentType(),
                roleEntity,
                user.isActive(),
                LocalDateTime.now()
        );
        UserEntity saved = userJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<User> findAllByIds(List<Long> ids) {
        return userJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public GetRolesStatsUseCase.RolesStats getRoleStats(Role role) {
        RoleEntity roleEntity = roleJpaRepository.findByName(role.name())
                .orElseThrow(() -> new IllegalStateException("Role not found: " + role.name()));

        long total = userJpaRepository.countByRole(roleEntity);
        long active = userJpaRepository.countByRoleAndActive(roleEntity, true);

        return new GetRolesStatsUseCase.RolesStats(role, total, active, total - active);
    }

    @Override
    public PageResult<User> findAllPaged(ListUsersQuery query) {
        Specification<UserEntity> spec = buildSpecification(query);
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));

        var page = userJpaRepository.findAll(spec, pageable);
        List<User> content = page.getContent().stream().map(this::toDomain).toList();

        return new PageResult<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public void deleteById(Long id) {
        userJpaRepository.deleteById(id);
    }

    private Specification<UserEntity> buildSpecification(ListUsersQuery query) {
        return (root, criteriaQuery, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query.search() != null && !query.search().isBlank()) {
                String trimmed = query.search().trim();
                String pattern = "%" + trimmed.toLowerCase() + "%";
                var searchPredicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>(List.of(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
                // A purely numeric search also matches the user's raw ID exactly,
                // so typing "7" finds user #7 in addition to any name/email match.
                try {
                    searchPredicates.add(cb.equal(root.get("id"), Long.parseLong(trimmed)));
                } catch (NumberFormatException ignored) {
                    // not a numeric search — id predicate simply doesn't apply
                }
                predicates.add(cb.or(searchPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }

            if (query.role() != null) {
                predicates.add(cb.equal(root.get("role").get("name"), query.role().name()));
            }

            if (query.active() != null) {
                predicates.add(cb.equal(root.get("active"), query.active()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private User toDomain(UserEntity entity) {
        Role role = Role.valueOf(entity.getRole().getName());
        return new User(entity.getId(), entity.getEmail(), entity.getPassword(),
                entity.getFirstName(), entity.getLastName(), role, entity.isActive(), entity.getCin(),
                entity.getProfilePictureKey(), entity.getProfilePictureContentType());
    }
}