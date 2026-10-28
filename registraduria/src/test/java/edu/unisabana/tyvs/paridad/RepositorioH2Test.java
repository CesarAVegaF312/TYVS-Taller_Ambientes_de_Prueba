package edu.unisabana.tyvs.paridad;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

/**
 * El contrato contra H2 en memoria. Rapido y sin Docker: por eso corre en
 * `mvn test`, en cualquier maquina.
 */
class RepositorioH2Test extends ContratoDelRepositorio {

    @Override
    protected DataSource dataSource() {
        // Una base distinta por prueba: aislamiento sin tener que limpiar.
        // (El DELETE del contrato sobra aqui, pero no estorba.)
        return new DriverManagerDataSource(
                "jdbc:h2:mem:paridad-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    /** H2 trata NULL como el valor MAS BAJO: aparece primero en orden ascendente. */
    @Override
    protected List<String> ordenObservadoConMunicipioNulo() {
        return List.of("Sin municipio", "Bogota", "Chia");
    }
}
