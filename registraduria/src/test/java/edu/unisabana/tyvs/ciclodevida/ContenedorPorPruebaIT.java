package edu.unisabana.tyvs.ciclodevida;

import edu.unisabana.tyvs.ambiente.Imagenes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MODULO 3 — Un ambiente NUEVO por cada prueba.
 *
 * Maximo aislamiento: ninguna prueba puede ver lo que dejo otra, porque cada
 * una tiene su propio PostgreSQL. El precio es el arranque, que se paga N
 * veces. Esta clase lo mide y lo imprime; compare con ContenedorCompartidoIT.
 *
 * El contenedor se maneja a mano (y no con @Container) solo para poder medir
 * cuanto tarda en arrancar.
 */
@Testcontainers(disabledWithoutDocker = true)
class ContenedorPorPruebaIT {

    private static long msTotalesDeArranque;
    private static int arranques;

    private PostgreSQLContainer postgres;

    @BeforeEach
    void arrancarUnContenedorNuevo() {
        long inicio = System.nanoTime();
        postgres = new PostgreSQLContainer(DockerImageName.parse(Imagenes.POSTGRES));
        postgres.start();
        msTotalesDeArranque += (System.nanoTime() - inicio) / 1_000_000;
        arranques++;
    }

    @AfterEach
    void detenerlo() {
        postgres.stop();
    }

    @Test
    void primeraConsulta() throws Exception {
        assertTrue(responde());
    }

    @Test
    void segundaConsulta() throws Exception {
        assertTrue(responde());
    }

    @Test
    void terceraConsulta() throws Exception {
        assertTrue(responde());
    }

    @AfterAll
    static void informe() {
        System.out.printf("[ciclo de vida] POR PRUEBA: %d arranques, %d ms en total, %d ms por prueba%n",
                arranques, msTotalesDeArranque, arranques == 0 ? 0 : msTotalesDeArranque / arranques);
    }

    private boolean responde() throws Exception {
        try (Connection c = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             ResultSet rs = c.createStatement().executeQuery("SELECT 1")) {
            return rs.next() && rs.getInt(1) == 1;
        }
    }
}
