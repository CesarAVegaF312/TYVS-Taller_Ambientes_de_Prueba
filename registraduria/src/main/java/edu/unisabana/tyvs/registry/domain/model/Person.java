package edu.unisabana.tyvs.registry.domain.model;

/**
 * Persona que pide inscribirse.
 *
 * {@code municipio} es opcional (puede ser null) y no participa en las reglas
 * de inscripcion. Esta aqui por el modulo 2: un valor nulo es exactamente el
 * tipo de dato en el que H2 y PostgreSQL no se comportan igual.
 */
public record Person(String name, int id, int age, boolean alive, String municipio) {

    public Person(String name, int id, int age, boolean alive) {
        this(name, id, age, alive, null);
    }
}
