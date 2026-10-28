package edu.unisabana.tyvs.ambiente;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MODULO 1 — El ambiente tambien se prueba.
 *
 * El ambiente de este taller esta escrito como codigo: docker-compose.yml,
 * registraduria/Dockerfile y .devcontainer/devcontainer.json. Y el codigo que
 * nadie verifica se degrada. La degradacion tipica de un ambiente se llama
 * DERIVA: una imagen sin version fija ("postgres:latest") que hoy es la 16 y el
 * mes que viene la 17, sin que nadie haya cambiado una linea del repositorio.
 *
 * Estas pruebas leen esos archivos como texto (no necesitan Docker) y fallan si
 * el ambiente deja de estar fijado.
 */
class AmbienteFijadoTest {

    /** Surefire ejecuta desde registraduria/; la raiz del repo esta un nivel arriba. */
    private static final Path RAIZ = Path.of("..");

    private static final Pattern IMAGE_COMPOSE = Pattern.compile("(?m)^\\s*image:\\s*\"?([^\"\\s#]+)");
    private static final Pattern FROM_DOCKERFILE = Pattern.compile("(?mi)^FROM\\s+(\\S+)");
    private static final Pattern IMAGE_DEVCONTAINER = Pattern.compile("\"image\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    void lasImagenesDelComposeTienenVersionExplicita() throws IOException {
        List<String> imagenes = buscar(IMAGE_COMPOSE, leer("docker-compose.yml"));
        assertFalse(imagenes.isEmpty(), "docker-compose.yml deberia declarar al menos una imagen");
        imagenes.forEach(AmbienteFijadoTest::exigirVersionFija);
    }

    @Test
    void lasImagenesDelDockerfileTienenVersionExplicita() throws IOException {
        List<String> bases = buscar(FROM_DOCKERFILE, leer("registraduria/Dockerfile"));
        assertFalse(bases.isEmpty(), "el Dockerfile deberia tener al menos un FROM");
        bases.forEach(AmbienteFijadoTest::exigirVersionFija);
    }

    @Test
    void elDevcontainerNoUsaLatest() throws IOException {
        List<String> imagenes = buscar(IMAGE_DEVCONTAINER, leer(".devcontainer/devcontainer.json"));
        assertEquals(1, imagenes.size(), "devcontainer.json deberia declarar una imagen");
        exigirVersionFija(imagenes.get(0));
    }

    /**
     * Paridad entre lo que se prueba y lo que se despliega: el PostgreSQL de
     * las pruebas (Testcontainers) debe ser exactamente el del compose.
     */
    @Test
    void elPostgresDelComposeEsElMismoDeLasPruebas() throws IOException {
        List<String> postgres = buscar(IMAGE_COMPOSE, leer("docker-compose.yml")).stream()
                .filter(i -> i.startsWith("postgres:"))
                .toList();
        assertEquals(List.of(Imagenes.POSTGRES), postgres,
                "docker-compose.yml y Imagenes.POSTGRES deben usar la misma imagen de PostgreSQL");
    }

    private static void exigirVersionFija(String imagen) {
        int dosPuntos = imagen.lastIndexOf(':');
        assertTrue(dosPuntos > 0 && dosPuntos > imagen.lastIndexOf('/'),
                () -> imagen + " no tiene version: equivale a :latest");
        String version = imagen.substring(dosPuntos + 1);
        assertFalse(version.equalsIgnoreCase("latest"),
                () -> imagen + " usa :latest, que cambia sin que cambie el repositorio");
    }

    private static String leer(String relativo) throws IOException {
        return Files.readString(RAIZ.resolve(relativo));
    }

    private static List<String> buscar(Pattern patron, String texto) {
        List<String> encontrados = new ArrayList<>();
        Matcher m = patron.matcher(texto);
        while (m.find()) {
            encontrados.add(m.group(1));
        }
        return encontrados;
    }
}
