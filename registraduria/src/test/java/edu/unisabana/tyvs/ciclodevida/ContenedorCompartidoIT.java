package edu.unisabana.tyvs.ciclodevida;

import edu.unisabana.tyvs.ambiente.Imagenes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MODULO 3 — UN ambiente compartido por toda la clase.
 *
 * El arranque se paga una vez. A cambio, las pruebas comparten base de datos:
 * si una deja datos, la siguiente los ve. El aislamiento ya no es gratis; hay
 * que construirlo (limpiar antes de cada prueba, como hace
 * ContratoDelRepositorio, o usar datos que no choquen).
 */
@Testcontainers(disabledWithoutDocker = true)
class ContenedorCompartidoIT {

    private static PostgreSQLContainer postgres;
    private static long msDeArranque;

    @BeforeAll
    static void arrancarUnaSolaVez() {
        long inicio = System.nanoTime();
        postgres = new PostgreSQLContainer(DockerImageName.parse(Imagenes.POSTGRES));
        postgres.start();
        msDeArranque = (System.nanoTime() - inicio) / 1_000_000;
    }

    @AfterAll
    static void detenerYInformar() {
        postgres.stop();
        System.out.printf("[ciclo de vida] COMPARTIDO: 1 arranque, %d ms en total, %d ms por prueba%n",
                msDeArranque, msDeArranque / 3);
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

    private static boolean responde() throws Exception {
        try (Connection c = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             ResultSet rs = c.createStatement().executeQuery("SELECT 1")) {
            return rs.next() && rs.getInt(1) == 1;
        }
    }
}
