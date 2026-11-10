package com.tarcinapp.epc;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — verifies that the Quarkus application context starts up
 * with all CDI beans resolvable. Backend health check is disabled in the
 * test profile (see application.properties: %test.backend.health-check.enabled=false)
 * so no live EPS or Redis is required.
 */
@QuarkusTest
public class EpcApplicationTest {

    @Test
    public void applicationStartsUp() {
        // If the CDI context starts and all beans are wired, this test passes.
        // No assertions needed — Quarkus will fail the test if any bean
        // cannot be resolved or if the application fails to boot.
    }
}
