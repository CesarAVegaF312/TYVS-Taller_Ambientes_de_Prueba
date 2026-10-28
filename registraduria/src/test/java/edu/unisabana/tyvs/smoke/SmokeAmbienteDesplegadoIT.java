package edu.unisabana.tyvs.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MODULO 6 — Smoke test contra un ambiente YA DESPLEGADO (por ejemplo, el de
 * docker compose). No arranca nada: se conecta a la URL que se le pase.
 *
 * Por defecto se OMITE. Se activa asi, con el compose arriba:
 *
 *   mvn test-compile failsafe:integration-test failsafe:verify \
 *       -Dit.test=SmokeAmbienteDesplegadoIT -Dambiente.url=http://localhost:8080
 *
 * Es la misma idea que se usa despues de cada despliegue real: antes de
 * mandarle trafico a un ambiente, comprobar que responde.
 */
@EnabledIfSystemProperty(named = "ambiente.url", matches = ".+")
class SmokeAmbienteDesplegadoIT {

    private static final String URL = System.getProperty("ambiente.url", "");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void elAmbienteDesplegadoEstaListo() throws Exception {
        HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(URL + "/actuator/health/readiness")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void elAmbienteDesplegadoAtiendeUnaInscripcion() throws Exception {
        // Id distinto en cada corrida: contra un ambiente persistente, un id
        // fijo daria VALID la primera vez y DUPLICATED las siguientes.
        int id = 100_000 + (int) (System.currentTimeMillis() % 800_000);
        String json = "{\"name\":\"Smoke\",\"id\":" + id + ",\"age\":30,\"alive\":true}";

        HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(URL + "/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(Set.of("\"VALID\"", "\"DUPLICATED\"")).contains(r.body());
    }
}
