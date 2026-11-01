package edu.unisabana.tyvs.paridad;

import edu.unisabana.tyvs.ambiente.Imagenes;
import edu.unisabana.tyvs.registry.RegistryApplication;
import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La aplicacion COMPLETA (Spring, Flyway, el caso de uso) contra PostgreSQL.
 *
 * @ServiceConnection (Spring Boot 3.1+) toma la URL, el usuario y la clave del
 * contenedor y los usa en lugar de los del perfil activo. Sin el, habria que
 * copiarlos a mano con @DynamicPropertySource.
 *
 * Fijese en lo que NO hay que configurar: el esquema. Flyway aplica al
 * arrancar las mismas migraciones que en dev y en prod.
 */
@SpringBootTest(classes = RegistryApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class AplicacionContraPostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse(Imagenes.POSTGRES));

    @Autowired
    private Registry registry;

    @Autowired
    private RegistryRepositoryPort repo;

    @Test
    void inscribeYDetectaElDuplicadoContraPostgres() {
        Person ana = new Person("Ana", 500, 30, true, "Chia");

        assertEquals(RegisterResult.VALID, registry.registerVoter(ana));
        assertEquals(RegisterResult.DUPLICATED, registry.registerVoter(ana));
        assertTrue(repo.existsById(500));
    }
}
