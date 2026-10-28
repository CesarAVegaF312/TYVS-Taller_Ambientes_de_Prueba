package edu.unisabana.tyvs.datos.defectuosas;

import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;
import edu.unisabana.tyvs.registry.infrastructure.persistence.JdbcRegistryRepository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DEFECTUOSA A PROPOSITO. No copie este patron a un proyecto real.
 *
 * Las dos pruebas comparten una base de datos y la segunda depende de lo que
 * dejo la primera. Con @Order fijado son verdes siempre... en ESTE ambiente.
 * Basta cambiar el ambiente de ejecucion para romperlas:
 *
 *   mvn test -Dtest='PruebaDependienteDelOrdenTest#segundaInscripcionDeAnaEsDuplicada'
 *
 * Ejecutada sola, la segunda prueba falla: Ana no esta inscrita, porque nadie
 * corrio la primera. Lo mismo pasa si un IDE, un filtro de CI o una
 * ejecucion en paralelo cambian el orden. El ejercicio del modulo 4 es
 * arreglarla sin depender del orden.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebaDependienteDelOrdenTest {

    private static Registry registry;

    @BeforeAll
    static void baseCompartida() {
        DataSource ds = new DriverManagerDataSource("jdbc:h2:mem:compartida;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        registry = new Registry(new JdbcRegistryRepository(ds));
    }

    @Test
    @Order(1)
    void inscribeAAna() {
        assertEquals(RegisterResult.VALID, registry.registerVoter(new Person("Ana", 100, 30, true)));
    }

    @Test
    @Order(2)
    void segundaInscripcionDeAnaEsDuplicada() {
        assertEquals(RegisterResult.DUPLICATED, registry.registerVoter(new Person("Ana", 100, 30, true)));
    }
}
