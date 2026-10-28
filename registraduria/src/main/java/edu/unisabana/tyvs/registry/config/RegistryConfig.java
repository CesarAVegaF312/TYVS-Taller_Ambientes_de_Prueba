package edu.unisabana.tyvs.registry.config;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.infrastructure.persistence.JdbcRegistryRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Cableado. El DataSource no se crea aqui: lo crea Spring Boot a partir de las
 * propiedades del perfil activo (application-dev o application-prod). Asi el
 * mismo codigo corre contra H2 o PostgreSQL sin recompilar: lo que cambia entre
 * ambientes es la CONFIGURACION, no el artefacto.
 */
@Configuration
public class RegistryConfig {

    @Bean
    public RegistryRepositoryPort registryRepositoryPort(DataSource dataSource) {
        return new JdbcRegistryRepository(dataSource);
    }

    @Bean
    public Registry registry(RegistryRepositoryPort port) {
        return new Registry(port);
    }
}
