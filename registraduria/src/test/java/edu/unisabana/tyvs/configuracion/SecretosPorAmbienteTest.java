package edu.unisabana.tyvs.configuracion;

import edu.unisabana.tyvs.registry.RegistryApplication;
import edu.unisabana.tyvs.registry.config.RegistraduriaProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MODULO 5 — Configuracion y secretos por ambiente.
 *
 * Lo que se prueba aqui no es una regla de negocio: es que la aplicacion se
 * NIEGUE a arrancar cuando el ambiente esta mal configurado (fail-fast), y que
 * la configuracion de produccion no traiga escondido un valor de desarrollo.
 */
class SecretosPorAmbienteTest {

    @Configuration
    @EnableConfigurationProperties(RegistraduriaProperties.class)
    static class SoloPropiedades {
    }

    /** Un contexto minimo, con solo las propiedades: rapido y sin base de datos. */
    private final ApplicationContextRunner contexto =
            new ApplicationContextRunner().withUserConfiguration(SoloPropiedades.class);

    @Test
    void sinSecretoLaAplicacionNoArranca() {
        contexto.run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void conUnSecretoDemasiadoCortoTampocoArranca() {
        contexto.withPropertyValues("registraduria.clave-auditoria=1234")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void conUnSecretoValidoArranca() {
        contexto.withPropertyValues("registraduria.clave-auditoria=una-clave-larga-de-prueba")
                .run(ctx -> assertThat(ctx).hasNotFailed()
                        .getBean(RegistraduriaProperties.class)
                        .extracting(RegistraduriaProperties::claveAuditoria)
                        .isEqualTo("una-clave-larga-de-prueba"));
    }

    /**
     * La trampa del placeholder: si la variable no existe, Spring Boot entrega
     * "${REGISTRADURIA_CLAVE_AUDITORIA}" como texto. Ese texto no esta en
     * blanco y es largo; solo el @Pattern de RegistraduriaProperties lo frena.
     */
    @Test
    void unPlaceholderSinResolverNoSirveComoSecreto() {
        contexto.withPropertyValues("registraduria.clave-auditoria=${REGISTRADURIA_CLAVE_AUDITORIA}")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    /**
     * La aplicacion completa en perfil prod, con base de datos pero SIN el
     * secreto: tiene que negarse a arrancar, y el error tiene que nombrar lo
     * que falta. (Las variables de la base se dan como propiedades por
     * defecto y apuntan a un H2: lo que se prueba aqui es el secreto.)
     */
    @Test
    void elPerfilProdSinSecretoNoArrancaYDiceQueFalta() {
        assertThatThrownBy(() -> prod("DB_URL=jdbc:h2:mem:prod-sin-secreto", "DB_USER=sa", "DB_PASSWORD="))
                .hasStackTraceContaining("claveAuditoria");
    }

    /** El control: con todo lo que pide, el perfil prod si arranca. */
    @Test
    void elPerfilProdConTodasSusVariablesArranca() {
        try (ConfigurableApplicationContext ctx = prod(
                "DB_URL=jdbc:h2:mem:prod-completo", "DB_USER=sa", "DB_PASSWORD=",
                "REGISTRADURIA_CLAVE_AUDITORIA=una-clave-larga-de-prueba")) {
            assertThat(ctx.isRunning()).isTrue();
        }
    }

    private static ConfigurableApplicationContext prod(String... variables) {
        return new SpringApplicationBuilder(RegistryApplication.class)
                .profiles("prod")
                .web(WebApplicationType.NONE)
                .properties(variables)
                .run();
    }

    /**
     * En application-prod.properties ningun ${VARIABLE} puede tener valor por
     * defecto (${VARIABLE:valor}). Un valor por defecto en prod convierte un
     * despliegue mal configurado en uno que arranca igual.
     */
    @Test
    void elPerfilProdNoTraeValoresPorDefectoParaSusVariables() throws Exception {
        Properties prod = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));
        Pattern placeholder = Pattern.compile("\\$\\{([^}]*)}");

        for (String clave : prod.stringPropertyNames()) {
            Matcher m = placeholder.matcher(prod.getProperty(clave));
            while (m.find()) {
                assertThat(m.group(1))
                        .as("%s usa un valor por defecto en prod", clave)
                        .doesNotContain(":");
            }
        }
    }

    /** El valor de desarrollo de la clave no puede aparecer en el perfil prod. */
    @Test
    void laClaveDeDesarrolloNoApareceEnProduccion() throws Exception {
        Properties dev = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-dev.properties"));
        Properties prod = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));

        String claveDev = dev.getProperty("registraduria.clave-auditoria");

        assertThat(prod.values()).doesNotContain(claveDev);
    }
}
