package edu.unisabana.tyvs.registry.infrastructure.persistence;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.domain.model.Person;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 * Adaptador JDBC. Funciona igual contra H2 y contra PostgreSQL... casi.
 *
 * Fijese en que NO crea la tabla: el esquema lo crean SIEMPRE las migraciones
 * de Flyway (src/main/resources/db/migration). Si este adaptador tuviera su
 * propio CREATE TABLE, habria dos definiciones del esquema y tarde o temprano
 * divergirian entre ambientes (modulo 4).
 */
public class JdbcRegistryRepository implements RegistryRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcRegistryRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean existsById(int id) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM votante WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    @Override
    public void save(Person p) {
        jdbc.update(
                "INSERT INTO votante (id, nombre, edad, vivo, municipio) VALUES (?, ?, ?, ?, ?)",
                p.id(), p.name(), p.age(), p.alive(), p.municipio());
    }

    @Override
    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM votante", Long.class);
        return n == null ? 0 : n;
    }

    /**
     * Consulta de reporte. Parece inocente y es el defecto latente del
     * modulo 2: el ORDER BY no dice donde van los municipios nulos, y cada
     * motor responde distinto a esa pregunta.
     */
    @Override
    public List<String> nombresOrdenadosPorMunicipio() {
        return jdbc.queryForList(
                "SELECT nombre FROM votante ORDER BY municipio, id", String.class);
    }
}
