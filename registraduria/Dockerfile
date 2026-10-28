# =====================================================================
# Imagen de la Registraduria, en dos etapas.
#
# Etapa 1 (build): compila con Maven. Trae JDK y Maven completos.
# Etapa 2 (runtime): solo el JRE y el jar. Lo que se despliega no lleva
# compilador, ni codigo fuente, ni el repositorio local de Maven.
#
# Todas las imagenes base con version EXACTA (lo comprueba
# AmbienteFijadoTest). "eclipse-temurin:17-jre" seria la 17 mas reciente: hoy
# una, manana otra.
# =====================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Primero solo el pom: Docker cachea esta capa y no vuelve a descargar las
# dependencias mientras el pom no cambie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17.0.12_7-jre-jammy
# curl solo para el healthcheck de docker-compose.yml.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# No correr como root dentro del contenedor.
RUN useradd --system --create-home registraduria
USER registraduria
WORKDIR /app

COPY --from=build /build/target/registraduria-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
