package com.idonice.mini_wsa.integration;

import com.idonice.mini_wsa.TestcontainersConfiguration;
import com.idonice.mini_wsa.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// End to end: real HTTP -> controllers -> services -> real PostgreSQL (Testcontainers).
// Each test starts from an empty table and creates its own data.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class EventApiIntegrationTest {

    private static final String ATTACKER_IP = "198.51.100.66";
    private static final Instant WAVE_START = Instant.parse("2026-11-16T09:00:00Z");
    private static final String DAY = "from=2026-11-16T00:00:00Z&to=2026-11-17T00:00:00Z";

    @LocalServerPort
    private int port;

    @Autowired
    private EventRepository eventRepository;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAllInBatch();
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void ingestReportsEachEventAndStoresEnrichedValues() {
        List<String> batch = new ArrayList<>(attackWave(7));
        batch.add(attackWaveEvent(1));                       // duplicate of wave-1
        batch.add("""
                {"eventId":"bad-1","timestamp":"2026-11-16T09:30:00Z","configId":1,
                 "path":"/x","rule":{"severity":"LOW","category":"BOT"},"action":"MONITOR"}
                """);                                         // invalid: no clientIp

        ingest(batch)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.received").isEqualTo(9)
                .jsonPath("$.ingested").isEqualTo(7)
                .jsonPath("$.duplicates").isEqualTo(1)
                .jsonPath("$.rejected.length()").isEqualTo(1)
                .jsonPath("$.rejected[0].index").isEqualTo(8)
                .jsonPath("$.rejected[0].errors[0]").isEqualTo("clientIp: must not be blank");

        // CRITICAL 40 + DENY 20 + /login 15 = 75; the 7th event also gets the repeat-offender +15
        client.get().uri("/v1/events/samples?limit=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.events[0].eventId").isEqualTo("wave-7")
                .jsonPath("$.events[0].attackType").isEqualTo("SQL/Command Injection")
                .jsonPath("$.events[0].threatScore").isEqualTo(90);
    }

    @Test
    void statsSummarizeTheIngestedEvents() {
        List<String> batch = new ArrayList<>(attackWave(7));
        batch.add("""
                {"eventId":"other-1","timestamp":"2026-11-16T08:00:00Z","configId":555,
                 "clientIp":"203.0.113.10","path":"/search",
                 "rule":{"severity":"HIGH","category":"XSS"},"action":"ALERT"}
                """);                                         // 30 + 10 = 40
        ingest(batch).expectStatus().isCreated();

        client.get().uri("/v1/stats/summary?" + DAY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalEvents").isEqualTo(8)
                .jsonPath("$.byAction.DENY").isEqualTo(7)
                .jsonPath("$.byAction.ALERT").isEqualTo(1)
                .jsonPath("$.byCategory.INJECTION.count").isEqualTo(7)
                .jsonPath("$.byCategory.INJECTION.avgThreatScore").isEqualTo(77.1)   // (6*75 + 90) / 7
                .jsonPath("$.topAttackers[0].clientIp").isEqualTo(ATTACKER_IP)
                .jsonPath("$.topAttackers[0].count").isEqualTo(7)
                .jsonPath("$.topTargetedPaths[0].path").isEqualTo("/login");

        client.get().uri("/v1/stats/summary?configId=555&" + DAY)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalEvents").isEqualTo(1)
                .jsonPath("$.byCategory.XSS.avgThreatScore").isEqualTo(40.0);
    }

    @Test
    void samplesAreFilteredPagedAndNewestFirst() {
        ingest(attackWave(7)).expectStatus().isCreated();

        client.get().uri("/v1/events/samples?limit=3&offset=3")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(7)
                .jsonPath("$.events.length()").isEqualTo(3)
                .jsonPath("$.events[0].eventId").isEqualTo("wave-4")       // page 2, newest first
                .jsonPath("$.events[2].eventId").isEqualTo("wave-2");

        client.get().uri("/v1/events/samples?category=XSS")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0)
                .jsonPath("$.events.length()").isEqualTo(0);
    }

    @Test
    void invalidRequestsGetProblemDetails() {
        client.post().uri("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .body("[]")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("A request must contain at least 1 event");

        client.get().uri("/v1/events/samples?limit=101")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("limit must be at most 100");

        client.get().uri("/v1/stats/summary?from=2026-11-17T00:00:00Z&to=2026-11-16T00:00:00Z")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("'from' must be before 'to'");
    }

    // ---------- helpers ----------

    private RestTestClient.ResponseSpec ingest(List<String> events) {
        return client.post().uri("/v1/events/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .body("[" + String.join(",", events) + "]")
                .exchange();
    }

    // One IP, one minute apart: CRITICAL + DENY on /login
    private List<String> attackWave(int size) {
        List<String> wave = new ArrayList<>();
        for (int n = 1; n <= size; n++) {
            wave.add(attackWaveEvent(n));
        }
        return wave;
    }

    private String attackWaveEvent(int n) {
        Instant timestamp = WAVE_START.plusSeconds(60L * (n - 1));
        return """
                {"eventId":"wave-%d","timestamp":"%s","configId":14227,"clientIp":"%s",
                 "path":"/login","method":"POST",
                 "rule":{"severity":"CRITICAL","category":"INJECTION"},"action":"DENY"}
                """.formatted(n, timestamp, ATTACKER_IP);
    }
}
