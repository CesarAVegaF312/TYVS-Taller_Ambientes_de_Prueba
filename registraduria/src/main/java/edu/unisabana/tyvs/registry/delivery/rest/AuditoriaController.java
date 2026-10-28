package edu.unisabana.tyvs.registry.delivery.rest;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.config.RegistraduriaProperties;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Endpoint protegido por el secreto del modulo 5. Existe para que el secreto
 * sirva para algo: un secreto que nada usa no se echa de menos cuando falta.
 */
@RestController
public class AuditoriaController {

    private final RegistryRepositoryPort repo;
    private final byte[] clave;

    public AuditoriaController(RegistryRepositoryPort repo, RegistraduriaProperties props) {
        this.repo = repo;
        this.clave = props.claveAuditoria().getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/auditoria/conteo")
    public ResponseEntity<Map<String, Long>> conteo(
            @RequestHeader(value = "X-Clave-Auditoria", required = false) String recibida) {
        // MessageDigest.isEqual compara en tiempo constante: no filtra por
        // cuanto tarda cuantos caracteres acerto el atacante.
        if (recibida == null
                || !MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("inscritos", repo.count()));
    }
}
