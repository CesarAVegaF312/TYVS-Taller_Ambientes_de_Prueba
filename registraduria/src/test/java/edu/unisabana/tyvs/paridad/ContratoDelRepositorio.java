package edu.unisabana.tyvs.paridad;

import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.infrastructure.persistence.JdbcRegistryRepository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MODULO 2 — Paridad de ambientes.
 *
 * Una sola bateria de pruebas, escrita una vez, que se ejecuta contra DOS
 * motores: H2 (RepositorioH2Test) y PostgreSQL (RepositorioPostgresIT). Cada
 * subclase solo dice como obtener un DataSource. Si una prueba pasa en un
 * motor y falla en el otro, encontro una diferencia entre ambientes, que es
 * exactamente lo que se busca.
 *
 * El esquema se crea con las MISMAS migraciones de Flyway que usa la
 * aplicacion. Si la prueba creara la tabla por su cuenta, estaria probando un
 * esquema que no existe en ningun ambiente real.
 */
public abstract class ContratoDelRepositorio {

    protected JdbcRegistryRepository repo;

    /** Cada motor entrega su DataSource. */
    protected abstract DataSource dataSource();

    /**
     * Lo que ESTE motor devuelve para la consulta de la prueba de divergencia.
     * Que cada subclase tenga que declararlo es la documentacion de la
     * diferencia.
     */
    protected abstract List<String> ordenObservadoConMunicipioNulo();

    @BeforeEach
    void prepararEsquemaLimpio() {
        DataSource ds = dataSource();
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        // Aislamiento (modulo 4): cada prueba empieza sin datos, sin importar
        // que dejo la anterior ni en que orden se ejecuten.
        new JdbcTemplate(ds).update("DELETE FROM votante");
        repo = new JdbcRegistryRepository(ds);
    }

    @Test
    void guardaYEncuentraPorId() {
        repo.save(new Person("Ana", 100, 30, true, "Chia"));
        assertTrue(repo.existsById(100));
    }

    @Test
    void noEncuentraUnIdQueNoSeGuardo() {
        assertFalse(repo.existsById(999));
    }

    @Test
    void cuentaLosRegistrosGuardados() {
        repo.save(new Person("Ana", 100, 30, true, "Chia"));
        repo.save(new Person("Luis", 101, 45, true, null));
        assertEquals(2, repo.count());
    }

    @Test
    void laBaseDeDatosRechazaUnIdRepetido() {
        repo.save(new Person("Ana", 100, 30, true, "Chia"));
        assertThrows(DataIntegrityViolationException.class,
                () -> repo.save(new Person("Otra Ana", 100, 31, true, "Cajica")));
    }

    /**
     * DIVERGENCIA DOCUMENTADA, no un contrato. La misma consulta, sobre los
     * mismos datos, devuelve un orden distinto segun el motor: el estandar SQL
     * no dice si NULL va antes o despues al ordenar, y cada motor eligio.
     *
     * Mientras esta prueba exista, el reporte de JdbcRegistryRepository
     * depende del ambiente. El ejercicio del modulo 2 es eliminar esa
     * dependencia.
     */
    @Test
    void divergenciaDocumentadaDondeQuedanLosNulosAlOrdenar() {
        repo.save(new Person("Chia", 1, 30, true, "Chia"));
        repo.save(new Person("Sin municipio", 2, 30, true, null));
        repo.save(new Person("Bogota", 3, 30, true, "Bogota"));

        assertEquals(ordenObservadoConMunicipioNulo(), repo.nombresOrdenadosPorMunicipio());
    }
}
