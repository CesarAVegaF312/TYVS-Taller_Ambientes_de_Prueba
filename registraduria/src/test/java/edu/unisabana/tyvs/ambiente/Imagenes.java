package edu.unisabana.tyvs.ambiente;

/**
 * Las imagenes de contenedor que usan las PRUEBAS, en un solo lugar.
 *
 * Si la version de PostgreSQL de Testcontainers y la de docker-compose.yml no
 * coinciden, las pruebas estan validando un motor y el ambiente corre otro.
 * AmbienteFijadoTest comprueba que coincidan.
 */
public final class Imagenes {

    /** Version exacta. Nunca "latest" y nunca solo la mayor ("postgres:16"). */
    public static final String POSTGRES = "postgres:16.4-alpine";

    private Imagenes() {
    }
}
