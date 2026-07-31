package ua.com.bravi.bravi;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCommand("postgres", "-c", "max_connections=300");

    static {
        POSTGRES.start();
    }
}
