package com.karim.gdmr_backend.auth.application;

import com.karim.gdmr_backend.auth.domain.exception.UserNotFoundException;
import com.karim.gdmr_backend.auth.domain.model.PageResult;
import com.karim.gdmr_backend.auth.domain.model.Role;
import com.karim.gdmr_backend.auth.domain.model.User;
import com.karim.gdmr_backend.auth.domain.port.in.*;
import com.karim.gdmr_backend.auth.domain.port.out.PasswordHasherPort;
import com.karim.gdmr_backend.auth.domain.port.out.RefreshTokenRepositoryPort;
import com.karim.gdmr_backend.auth.domain.port.out.TemporaryPasswordGeneratorPort;
import com.karim.gdmr_backend.auth.domain.port.out.UserRepositoryPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


@Service
@Transactional
public class AdminUserService implements CreateUserByAdminUseCase, UpdateUserUseCase, ListUsersUseCase, ChangeUserStatusUseCase, GetRolesStatsUseCase, GetUsersByIdsUseCase, DeleteUserUseCase {

    private static final String EMAIL_DOMAIN = "@sqli-gdmr.com";

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHasherPort passwordHasher;
    private final TemporaryPasswordGeneratorPort passwordGenerator;

    public AdminUserService(UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            TemporaryPasswordGeneratorPort passwordGenerator,
            RefreshTokenRepositoryPort refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.passwordGenerator = passwordGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public CreatedUserResult createUser(CreateUserCommand command) {
        String email = generateUniqueEmail(command.firstName(), command.lastName());
        String tempPassword = passwordGenerator.generate();
        String hashedPassword = passwordHasher.hash(tempPassword);

        User newUser = new User(null, email, hashedPassword,
                command.firstName(), command.lastName(), command.role(), true, command.cin(), null, null);

        User saved = userRepository.save(newUser);

        return new  CreatedUserResult(saved, tempPassword);
    }

    @Override
    public PageResult<User> listUsers(ListUsersQuery query) {
        return userRepository.findAllPaged(query);
    }

    @Override
    public List<User> getUsersByIds(List<Long> ids) {
        return userRepository.findAllByIds(ids);
    }

    @Override
    public User updateUser(UpdateUserCommand command) {
        User existing = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        User updated = existing.updateProfile(command.firstName(), command.lastName(), command.role(), command.cin());

        return userRepository.save(updated);
    }

    @Override
    public void changeUserStatus(ChangeUserStatusCommand command) {
        User userToChangeStatus = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        User updatedUser = userToChangeStatus.withActive(command.active());

        userRepository.save(updatedUser);

        if(!command.active()) {
            refreshTokenRepository.revokeAllByUserId(updatedUser.getId());
        }
    }

    @Override
    public List<GetRolesStatsUseCase.RolesStats> getRolesStats() {
        return Arrays.stream(Role.values())
                .map(userRepository::getRoleStats)
                .toList();
    }

    // Called last by AdminUserController.deleteUser, after the other modules' own
    // delete-by-user-id use cases have already removed the FK-dependent employee/doctor/
    // staff profile rows — this only touches auth-owned records plus the user row itself.
    @Override
    public void deleteUser(Long userId) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        refreshTokenRepository.deleteAllByUserId(existing.getId());
        userRepository.deleteById(existing.getId());
    }

    private String generateUniqueEmail(String firstName, String lastName) {
        String base = normalize(firstName).charAt(0) + "." + normalize(lastName);
        String candidate = base + EMAIL_DOMAIN;

        int suffix = 1;
        while (userRepository.existsByEmail(candidate)) {
            suffix++;
            candidate = base + suffix + EMAIL_DOMAIN;
        }
        return candidate;
    }

    private String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Pattern.compile("[^a-zA-Z]").matcher(withoutAccents).replaceAll("").toLowerCase();
    }


}
