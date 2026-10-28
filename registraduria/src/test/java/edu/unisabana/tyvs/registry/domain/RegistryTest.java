package edu.unisabana.tyvs.registry.domain;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Las reglas del dominio, con un repositorio en memoria. Estas pruebas no
 * dependen de NINGUN ambiente: corren igual en cualquier maquina, y por eso
 * son la linea base contra la que se comparan las demas.
 */
class RegistryTest {

    /** Doble de prueba: un mapa. Sin base de datos, sin red, sin Docker. */
    static class RepositorioEnMemoria implements RegistryRepositoryPort {
        private final Map<Integer, Person> datos = new HashMap<>();

        @Override public boolean existsById(int id) { return datos.containsKey(id); }
        @Override public void save(Person p) { datos.put(p.id(), p); }
        @Override public long count() { return datos.size(); }
        @Override public List<String> nombresOrdenadosPorMunicipio() { return new ArrayList<>(); }
    }

    private Registry registry;

    @BeforeEach
    void setUp() {
        registry = new Registry(new RepositorioEnMemoria());
    }

    @Test
    void personaValidaQuedaInscrita() {
        assertEquals(RegisterResult.VALID, registry.registerVoter(new Person("Ana", 1, 30, true)));
    }

    @Test
    void segundaInscripcionDelMismoIdEsDuplicada() {
        registry.registerVoter(new Person("Ana", 1, 30, true));
        assertEquals(RegisterResult.DUPLICATED, registry.registerVoter(new Person("Ana", 1, 30, true)));
    }

    @Test
    void menorDeEdadEsRechazado() {
        assertEquals(RegisterResult.UNDERAGE, registry.registerVoter(new Person("Luis", 2, 17, true)));
    }

    @Test
    void edadImposibleEsRechazada() {
        assertEquals(RegisterResult.INVALID_AGE, registry.registerVoter(new Person("Eva", 3, 121, true)));
    }

    @Test
    void personaFallecidaEsRechazada() {
        assertEquals(RegisterResult.DEAD, registry.registerVoter(new Person("Juan", 4, 40, false)));
    }

    @Test
    void idNoPositivoEsInvalido() {
        assertEquals(RegisterResult.INVALID, registry.registerVoter(new Person("Sin Id", 0, 40, true)));
    }

    @Test
    void personaNulaEsInvalida() {
        assertEquals(RegisterResult.INVALID, registry.registerVoter(null));
    }
}
