package com.example.demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExternalPersonIntegrationTest {

    private static final WireMockServer wireMock = startWireMock();

    @LocalServerPort
    private int port;

    private RestTestClient restTestClient;

    @BeforeEach
    void setUpRestTestClient() {
        restTestClient = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private static WireMockServer startWireMock() {
        var server = new WireMockServer(options()
                .port(9561)
                .usingFilesUnderDirectory(Path.of("wiremock").toAbsolutePath().toString()));
        server.start();
        return server;
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void getsPersonWithoutOAuthToken() {
        restTestClient.get()
                .uri("/external/person")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> assertThat(result.getResponseBody())
                        .contains("Ada", "Lovelace"));

        wireMock.verify(getRequestedFor(urlEqualTo("/person"))
                .withoutHeader("Authorization"));
    }
}
