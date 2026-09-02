package com.karim.gdmr_backend.shared.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises every requestMatcher rule declared in {@link SecurityConfig} against a
 * no-op controller — the goal isn't to test any real endpoint's business logic, only
 * that the URL/method/role matrix in SecurityConfig grants and denies access exactly
 * as intended. A 200 means the request cleared the security filter chain and reached
 * the controller; a 403/401 means it didn't.
 * <p>
 * Deliberately DB-free: only {@link SecurityConfig} and {@link JwtAuthFilter} are
 * imported into the slice, so this never needs a running Postgres instance.
 */
@WebMvcTest(controllers = ProbeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    private enum Role { ADMIN, HR, DOCTOR, EMPLOYEE }

    private static final Set<Role> ALL_ROLES = EnumSet.allOf(Role.class);

    private record Rule(HttpMethod method, String path, Set<Role> allowedRoles) {
        Rule(HttpMethod method, String path, Role... allowed) {
            this(method, path, allowed.length == 0 ? Set.of() : EnumSet.copyOf(List.of(allowed)));
        }
    }

    private static Stream<Rule> protectedRules() {
        return Stream.of(
                new Rule(HttpMethod.GET, "/api/admin/users", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.DELETE, "/api/admin/users/1", Role.ADMIN),
                new Rule(HttpMethod.POST, "/api/admin/users", Role.ADMIN),

                new Rule(HttpMethod.POST, "/api/visits/spontaneous", Role.EMPLOYEE),
                new Rule(HttpMethod.POST, "/api/visits/scheduled", Role.HR),
                new Rule(HttpMethod.POST, "/api/visits/send-reminders", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.PATCH, "/api/visits/1/assign-slot", Role.HR),
                new Rule(HttpMethod.PATCH, "/api/visits/1/confirm", Role.EMPLOYEE),
                new Rule(HttpMethod.PATCH, "/api/visits/1/employee-reject", Role.EMPLOYEE),
                new Rule(HttpMethod.PATCH, "/api/visits/1/doctor-confirm", Role.DOCTOR),
                new Rule(HttpMethod.PATCH, "/api/visits/1/doctor-reject", Role.DOCTOR),
                new Rule(HttpMethod.PATCH, "/api/visits/1/status", Role.DOCTOR),
                new Rule(HttpMethod.PATCH, "/api/visits/1/report", Role.DOCTOR),
                new Rule(HttpMethod.GET, "/api/visits/1/negotiation-history",
                        Role.ADMIN, Role.HR, Role.DOCTOR, Role.EMPLOYEE),
                new Rule(HttpMethod.GET, "/api/visits/1", Role.ADMIN, Role.HR, Role.DOCTOR, Role.EMPLOYEE),
                new Rule(HttpMethod.GET, "/api/visits", Role.ADMIN, Role.HR, Role.DOCTOR, Role.EMPLOYEE),

                new Rule(HttpMethod.GET, "/api/staff/profile", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.GET, "/api/staff/profile/1", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.PUT, "/api/staff/profile", Role.ADMIN),
                new Rule(HttpMethod.PUT, "/api/staff/profile/1", Role.ADMIN),

                new Rule(HttpMethod.POST, "/api/profile-issues", Role.EMPLOYEE, Role.DOCTOR, Role.HR),
                new Rule(HttpMethod.GET, "/api/profile-issues", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.PATCH, "/api/profile-issues/1/resolve", Role.ADMIN, Role.HR),

                new Rule(HttpMethod.GET, "/api/employee/profile", Role.EMPLOYEE),
                new Rule(HttpMethod.PUT, "/api/employee/profile", Role.EMPLOYEE),
                new Rule(HttpMethod.GET, "/api/employee/profile/1", Role.ADMIN, Role.HR, Role.DOCTOR),
                new Rule(HttpMethod.PUT, "/api/employee/profile/1", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.GET, "/api/employees", Role.ADMIN, Role.HR, Role.DOCTOR),
                new Rule(HttpMethod.GET, "/api/employees/my-patients", Role.ADMIN, Role.HR, Role.DOCTOR),
                new Rule(HttpMethod.PUT, "/api/employee/1", Role.ADMIN, Role.HR),

                new Rule(HttpMethod.GET, "/api/doctor/profile", Role.DOCTOR),
                new Rule(HttpMethod.PUT, "/api/doctor/profile", Role.DOCTOR),
                new Rule(HttpMethod.GET, "/api/doctor/profile/1", Role.ADMIN, Role.HR),
                new Rule(HttpMethod.GET, "/api/doctors", Role.ADMIN, Role.HR, Role.EMPLOYEE),
                new Rule(HttpMethod.PUT, "/api/doctor/1", Role.ADMIN, Role.HR),

                new Rule(HttpMethod.POST, "/api/timeslot", Role.HR),
                new Rule(HttpMethod.GET, "/api/timeslots", Role.HR, Role.EMPLOYEE),

                new Rule(HttpMethod.GET, "/api/documents/1", Role.DOCTOR, Role.EMPLOYEE, Role.HR),
                new Rule(HttpMethod.GET, "/api/medical-history/1", Role.DOCTOR),

                new Rule(HttpMethod.GET, "/api/notifications", Role.ADMIN, Role.HR, Role.DOCTOR, Role.EMPLOYEE),
                new Rule(HttpMethod.PATCH, "/api/notifications/1/read", Role.ADMIN, Role.HR, Role.DOCTOR, Role.EMPLOYEE),

                // Catch-all: anyRequest().authenticated() — any logged-in role, any path
                // not covered by a more specific rule above.
                new Rule(HttpMethod.GET, "/api/some-unmapped-endpoint",
                        Role.ADMIN, Role.HR, Role.DOCTOR, Role.EMPLOYEE)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} {1} -> allowed={2}")
    @MethodSource("protectedRules")
    void enforcesRoleMatrix(Rule rule) throws Exception {
        mockMvc.perform(request(rule.method(), rule.path()))
                .andExpect(status().isUnauthorized());

        for (Role role : ALL_ROLES) {
            int status = mockMvc.perform(request(rule.method(), rule.path())
                            .with(user("test-" + role).roles(role.name())))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            int expected = rule.allowedRoles().contains(role) ? 200 : 403;
            assertEquals(expected, status,
                    () -> rule.method() + " " + rule.path() + " as " + role
                            + " expected " + expected + " but got " + status);
        }
    }

    @ParameterizedTest(name = "permitAll: {0} {1}")
    @MethodSource("permitAllRoutes")
    void permitsWithoutAuthentication(HttpMethod method, String path) throws Exception {
        mockMvc.perform(request(method, path)).andExpect(status().isOk());
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> permitAllRoutes() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/auth/login"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.POST, "/api/auth/refresh"),
                org.junit.jupiter.params.provider.Arguments.of(HttpMethod.GET, "/api/notifications/stream")
        );
    }
}
