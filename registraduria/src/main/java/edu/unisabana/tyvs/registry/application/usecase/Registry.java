package edu.unisabana.tyvs.registry.application.usecase;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;

/**
 * Caso de uso: inscribir un votante.
 *
 * Las reglas ya estan completas y probadas. En este taller el objeto de estudio
 * no es la logica, sino los AMBIENTES donde esa logica se ejecuta.
 */
public class Registry {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 120;

    private final RegistryRepositoryPort repo;

    public Registry(RegistryRepositoryPort repo) {
        this.repo = repo;
    }

    public RegisterResult registerVoter(Person p) {
        if (p == null || p.id() <= 0) {
            return RegisterResult.INVALID;
        }
        if (!p.alive()) {
            return RegisterResult.DEAD;
        }
        if (p.age() < 0 || p.age() > MAX_AGE) {
            return RegisterResult.INVALID_AGE;
        }
        if (p.age() < MIN_AGE) {
            return RegisterResult.UNDERAGE;
        }
        if (repo.existsById(p.id())) {
            return RegisterResult.DUPLICATED;
        }
        repo.save(p);
        return RegisterResult.VALID;
    }
}
