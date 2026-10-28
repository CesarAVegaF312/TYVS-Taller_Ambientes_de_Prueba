package edu.unisabana.tyvs.smoke;

import edu.unisabana.tyvs.registry.RegistryApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MODULO 6 — Smoke test de la aplicacion arrancada (perfil dev, puerto
 * aleatorio).
 *
 * Un smoke test no prueba reglas: pregunta "esta vivo, esta listo y responde
 * lo basico". Si falla, no tiene sentido correr nada mas sobre este ambiente.
 */
@SpringBootTest(classes = RegistryApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AmbienteSmokeIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void laAplicacionEstaViva() {
        ResponseEntity<String> r = rest.getForEntity("/actuator/health/liveness", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void laAplicacionEstaListaParaRecibirTrafico() {
        ResponseEntity<String> r = rest.getForEntity("/actuator/health/readiness", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void inscribeUnaPersonaDeExtremoAExtremo() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"name\":\"Ana\",\"id\":321,\"age\":30,\"alive\":true,\"municipio\":\"Chia\"}";

        ResponseEntity<String> r = rest.postForEntity("/register", new HttpEntity<>(json, h), String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo("\"VALID\"");
    }

    @Test
    void laAuditoriaSinClaveEsRechazada() {
        ResponseEntity<String> r = rest.getForEntity("/auditoria/conteo", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
