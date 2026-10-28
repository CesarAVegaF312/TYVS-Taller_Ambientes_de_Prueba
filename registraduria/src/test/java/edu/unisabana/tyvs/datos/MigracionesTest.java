package edu.unisabana.tyvs.datos;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MODULO 4 — Las migraciones son la unica fuente del esquema.
 */
class MigracionesTest {

    private static DataSource baseNueva() {
        return new DriverManagerDataSource("jdbc:h2:mem:migr-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    @Test
    void lasMigracionesLlevanUnaBaseVaciaHastaLaVersion2() {
        Flyway flyway = Flyway.configure().dataSource(baseNueva()).locations("classpath:db/migration").load();

        flyway.migrate();

        assertEquals("2", flyway.info().current().getVersion().getVersion());
    }

    /**
     * Los datos semilla son para dev. Si llegaran a prod, el padron real
     * tendria tres votantes inventados. Se verifica sobre la configuracion
     * efectiva del perfil prod: la comun MAS la del perfil.
     */
    @Test
    void laSemillaNoLlegaAlPerfilDeProduccion() throws Exception {
        Properties comun = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        Properties prod = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));

        String ubicaciones = prod.getProperty("spring.flyway.locations",
                comun.getProperty("spring.flyway.locations"));

        assertFalse(ubicaciones.contains("semilla"),
                "el perfil prod no debe cargar db/semilla, pero sus ubicaciones son: " + ubicaciones);
    }

    /**
     * Una migracion repetible (R__) se vuelve a aplicar cada vez que cambia.
     * Si no fuera idempotente, cada cambio duplicaria los datos semilla.
     */
    @Test
    void laSemillaEsIdempotente() throws Exception {
        DataSource ds = baseNueva();
        Flyway.configure().dataSource(ds)
                .locations("classpath:db/migration", "classpath:db/semilla")
                .load().migrate();

        try (Connection c = ds.getConnection()) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/semilla/R__votantes_semilla.sql"));
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/semilla/R__votantes_semilla.sql"));
        }

        Integer semilla = new JdbcTemplate(ds).queryForObject(
                "SELECT COUNT(*) FROM votante WHERE id BETWEEN 9000 AND 9099", Integer.class);
        assertEquals(3, semilla);
    }

    /** La tabla no tiene datos hasta que alguien los inserta: ni la V1 ni la V2 insertan. */
    @Test
    void lasMigracionesDeEsquemaNoInsertanDatos() {
        DataSource ds = baseNueva();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();

        assertNull(new JdbcTemplate(ds).queryForObject("SELECT MIN(id) FROM votante", Integer.class));
    }
}
