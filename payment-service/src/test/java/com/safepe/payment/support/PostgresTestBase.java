package com.safepe.payment.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One PostgreSQL 15 container shared by every database test in this module
 * (Testcontainers "singleton container" pattern).
 * <p>
 * It is deliberately not managed by @Testcontainers/@Container: that stops the
 * container after each test class while Spring's cached context still holds a
 * connection pool to it, and closing that pool at JVM exit then hangs until
 * Surefire kills the JVM (which also truncates the JaCoCo coverage file).
 * Testcontainers' Ryuk sidecar removes this container once the JVM exits.
 */
public abstract class PostgresTestBase {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    static {
        POSTGRES.start();
    }
}
