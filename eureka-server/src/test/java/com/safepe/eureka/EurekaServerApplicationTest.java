package com.safepe.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** The registry starts and serves its application list. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTest {

    private final TestRestTemplate http;

    EurekaServerApplicationTest(TestRestTemplate http) {
        this.http = http;
    }

    @Test
    void startsAndServesTheRegistry() {
        assertThat(http.getForEntity("/eureka/apps", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }
}
