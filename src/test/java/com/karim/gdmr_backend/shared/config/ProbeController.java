package com.karim.gdmr_backend.shared.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only no-op controller for {@link SecurityConfigTest} — every request that
 * clears the security filter chain lands here and gets a plain 200, so the test can
 * assert purely on SecurityConfig's role matrix without any real business logic.
 */
@RestController
public class ProbeController {

    @RequestMapping(
            path = "/**",
            method = {
                    RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.PATCH, RequestMethod.DELETE
            })
    public ResponseEntity<Void> probe() {
        return ResponseEntity.ok().build();
    }
}
