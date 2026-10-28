package edu.unisabana.tyvs.registry.application.port.out;

import edu.unisabana.tyvs.registry.domain.model.Person;

import java.util.List;

/**
 * Puerto de salida. El caso de uso no sabe si detras hay H2, PostgreSQL o un
 * mapa en memoria; las pruebas del modulo 2 si lo saben, y por eso existen.
 */
public interface RegistryRepositoryPort {

    boolean existsById(int id);

    void save(Person person);

    long count();

    /** Nombres de los inscritos, ordenados por municipio. */
    List<String> nombresOrdenadosPorMunicipio();
}
