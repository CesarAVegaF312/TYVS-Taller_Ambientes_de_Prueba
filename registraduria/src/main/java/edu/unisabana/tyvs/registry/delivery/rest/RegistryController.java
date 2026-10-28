package edu.unisabana.tyvs.registry.delivery.rest;

import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistryController {

    private final Registry registry;

    public RegistryController(Registry registry) {
        this.registry = registry;
    }

    @PostMapping("/register")
    public RegisterResult register(@RequestBody PersonDTO dto) {
        return registry.registerVoter(
                new Person(dto.name(), dto.id(), dto.age(), dto.alive(), dto.municipio()));
    }
}
