package edu.unisabana.tyvs.paridad;

import edu.unisabana.tyvs.ambiente.Imagenes;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;

/**
 * El MISMO contrato contra un PostgreSQL real, en un contenedor efimero.
 *
 * disabledWithoutDocker = true: en una maquina sin Docker estas pruebas
 * aparecen como OMITIDAS, no como fallidas. Es honesto (no se ejecutaron) y no
 * bloquea a quien no puede instalar Docker. En CI si hay Docker, y ahi corren.
 */
@Testcontainers(disabledWithoutDocker = true)
class RepositorioPostgresIT extends ContratoDelRepositorio {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse(Imagenes.POSTGRES));

    @Override
    protected DataSource dataSource() {
        return new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /** PostgreSQL trata NULL como el valor MAS ALTO: aparece al final en orden ascendente. */
    @Override
    protected List<String> ordenObservadoConMunicipioNulo() {
        return List.of("Bogota", "Chia", "Sin municipio");
    }
}
