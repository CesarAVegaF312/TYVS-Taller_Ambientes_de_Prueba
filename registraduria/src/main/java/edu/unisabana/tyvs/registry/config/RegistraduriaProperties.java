package edu.unisabana.tyvs.registry.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion propia de la aplicacion (modulo 5).
 *
 * {@code claveAuditoria} es un SECRETO: protege el endpoint de auditoria. Se
 * valida al arrancar, y no la primera vez que alguien llama al endpoint, por
 * una razon concreta: un servicio que arranca "bien" sin su secreto falla a las
 * tres de la manana, en produccion, en la primera peticion real. Uno que se
 * niega a arrancar falla en el despliegue, delante de quien lo esta haciendo.
 * Eso es fail-fast.
 *
 * OJO con la trampa que motiva el @Pattern: si la variable de entorno no
 * existe, Spring Boot NO falla al leer "${REGISTRADURIA_CLAVE_AUDITORIA}". Lo
 * entrega tal cual, como texto. Y ese texto no esta en blanco y tiene mas de
 * 16 caracteres: sin el @Pattern, la aplicacion arrancaria en produccion con
 * una "clave" que es literalmente el nombre de la variable.
 */
@Validated
@ConfigurationProperties(prefix = "registraduria")
public record RegistraduriaProperties(

        @NotBlank(message = "falta el secreto registraduria.clave-auditoria")
        @Size(min = 16, message = "registraduria.clave-auditoria debe tener al menos 16 caracteres")
        @Pattern(regexp = "^(?!\\$\\{).*",
                message = "registraduria.clave-auditoria llego sin resolver: falta la variable de entorno")
        String claveAuditoria) {
}
