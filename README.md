# Taller de Ambientes de Prueba

"En mi máquina funciona" no es una excusa: es un **diagnóstico**. Dice que el
código es el mismo y el resultado no, así que la diferencia está en otra parte:
en el **ambiente**.

Una prueba no se ejecuta en el vacío. Se ejecuta sobre un sistema operativo,
una versión de Java, un motor de base de datos con ciertos datos, unas
variables de entorno y un orden de ejecución. Si cualquiera de esas cosas
cambia entre su máquina, la de su compañero, CI y producción, una prueba verde
deja de significar lo que usted cree.

Los talleres anteriores tocaron el problema de lado: H2 no es PostgreSQL
(Integración), las referencias visuales de Windows no sirven en Linux (UX),
Testcontainers es lento (Economía). Este taller lo pone en el centro: **el
ambiente también es código, y también se prueba**.

---

## Objetivos

- Escribir el ambiente **como código** (devcontainer, Dockerfile,
  docker-compose) y detectar con pruebas cuándo **deriva**.
- Medir la **paridad** entre ambientes: ejecutar la misma batería de pruebas
  contra H2 y contra PostgreSQL, y encontrar dónde no responden igual.
- Decidir el **ciclo de vida** de un ambiente efímero (uno por prueba o uno
  compartido) con números, no con intuición.
- Gestionar los **datos de prueba**: migraciones como única fuente del esquema,
  datos semilla que no llegan a producción, y pruebas que no dependen del orden.
- Separar la **configuración por ambiente** y hacer que la aplicación se niegue
  a arrancar (*fail-fast*) cuando le falta un secreto.
- Comprobar con un **smoke test** que un ambiente levantado está vivo y listo
  antes de usarlo.

---

## Tabla de contenido

- [Sistema bajo prueba](#sistema-bajo-prueba)
- [Puesta en marcha](#puesta-en-marcha)
- [Módulos del taller](#módulos-del-taller)
  - [Módulo 1 — Ambiente como código](#módulo-1--ambiente-como-código-ambientefijadotest)
  - [Módulo 2 — Paridad de ambientes](#módulo-2--paridad-de-ambientes-contratodelrepositorio)
  - [Módulo 3 — Ambientes efímeros y su ciclo de vida](#módulo-3--ambientes-efímeros-y-su-ciclo-de-vida-contenedorporpruebait-y-contenedorcompartidoit)
  - [Módulo 4 — Datos de prueba](#módulo-4--datos-de-prueba-migracionestest)
  - [Módulo 5 — Configuración y secretos por ambiente](#módulo-5--configuración-y-secretos-por-ambiente-secretosporambientetest)
  - [Módulo 6 — Smoke test del ambiente](#módulo-6--smoke-test-del-ambiente-ambientesmokeit-y-smokeambientedesplegadoit)
- [PARA ENTREGAR CON ESTE TALLER](#para-entregar-con-este-taller)
- [Rúbrica](#rúbrica)
- [Créditos y uso académico](#créditos-y-uso-académico)
- [Licencia de uso](#licencia-de-uso)

---

## Sistema bajo prueba

Una versión mínima de la `registraduria` de los talleres anteriores, con las
reglas ya implementadas (`VALID`, `DUPLICATED`, `UNDERAGE`, `INVALID_AGE`,
`DEAD`, `INVALID`). **La lógica no es el objeto de estudio: lo son los
ambientes donde corre.**

Este taller usa **Spring Boot 3.5** y Java 17, a diferencia de los anteriores
(Spring Boot 2.7). No es un detalle: la versión del framework también es parte
del ambiente.

```text
.
├─ .devcontainer/devcontainer.json     # módulo 1: ambiente de desarrollo
├─ docker-compose.yml                  # módulos 1 y 6: app + PostgreSQL
├─ .env.ejemplo                        # módulo 5: variables (sin secretos reales)
├─ .github/workflows/ambientes.yml     # CI: pruebas con Docker + smoke del compose
└─ registraduria/
    ├─ Dockerfile                      # módulo 1: imagen en dos etapas
    ├─ src/main/resources/
    │   ├─ application.properties      # lo común a todos los ambientes
    │   ├─ application-dev.properties  # H2 + datos semilla
    │   ├─ application-prod.properties # PostgreSQL + secretos por variable
    │   └─ db/
    │       ├─ migration/              # módulo 4: V1, V2 (el esquema)
    │       └─ semilla/                # módulo 4: datos solo para dev
    └─ src/test/java/edu/unisabana/tyvs/
        ├─ registry/domain/RegistryTest.java      # las reglas (línea base)
        ├─ ambiente/AmbienteFijadoTest.java       # módulo 1
        ├─ paridad/                               # módulo 2
        ├─ ciclodevida/                           # módulo 3
        ├─ datos/                                 # módulo 4
        │   └─ defectuosas/                       # DEFECTUOSAS A PROPÓSITO
        ├─ configuracion/                         # módulo 5
        └─ smoke/                                 # módulo 6
```

---

## Puesta en marcha

Hay tres formas de trabajar, de menos a más ambiente. **La primera basta para
aprobar**; las otras dos son el tema del taller.

### Opción A — Solo Maven (sin Docker)

```bash
cd registraduria
mvn clean verify
```

Resultado esperado: `BUILD SUCCESS` con **30 pruebas unitarias** y **18 de
integración**, de las cuales **14 aparecen omitidas**:

- 12 necesitan Docker (PostgreSQL con Testcontainers). Están marcadas con
  `@Testcontainers(disabledWithoutDocker = true)`: sin Docker se **omiten**, no
  fallan. Es honesto —no se ejecutaron— y no bloquea a quien no puede instalar
  Docker.
- 2 son el smoke test contra un ambiente desplegado, que solo corre si se le
  pasa una URL (módulo 6).

Con Docker instalado, `mvn clean verify` ejecuta las 12 de Testcontainers y
solo quedan omitidas las 2 del smoke.

### Opción B — Devcontainer (VS Code o GitHub Codespaces)

El archivo [`.devcontainer/devcontainer.json`](.devcontainer/devcontainer.json)
describe el ambiente de desarrollo completo: JDK 17, Maven 3.9.9 y Docker.

- **VS Code**: instale la extensión *Dev Containers*, abra la carpeta y elija
  *Reopen in Container*. Requiere Docker en su máquina.
- **GitHub Codespaces**: en GitHub, botón *Code* → *Codespaces* → *Create
  codespace*. No requiere instalar nada; corre en la nube y tiene una cuota
  gratuita mensual (ampliada con GitHub Education).

Dentro del contenedor, `mvn clean verify` ejecuta **todas** las pruebas de
Testcontainers, porque ahí sí hay Docker.

### Opción C — El ambiente completo con docker compose

```bash
cp .env.ejemplo .env            # y cambie los valores
docker compose up --build --wait
curl http://localhost:8080/actuator/health/readiness   # {"status":"UP"}
docker compose down -v          # al terminar
```

Levanta la Registraduría **en perfil `prod`** contra un PostgreSQL real.
`--wait` no devuelve el control hasta que los *healthchecks* estén en verde.

---

## Módulos del taller

### Módulo 1 — Ambiente como código ([`AmbienteFijadoTest`](registraduria/src/test/java/edu/unisabana/tyvs/ambiente/AmbienteFijadoTest.java))

Un ambiente descrito en un documento ("instale Java 17 y PostgreSQL") deriva
desde el día uno: cada quien instala una versión distinta. Un ambiente
descrito en **código** (devcontainer, Dockerfile, compose) se versiona, se
revisa en un *pull request* y se reproduce igual en cualquier máquina.

Pero solo si las versiones están **fijas**. Esta línea:

```yaml
image: postgres:latest
```

significa "PostgreSQL 16" hoy y "PostgreSQL 17" el mes que viene, sin que nadie
haya cambiado una línea del repositorio. Eso es **deriva**: el ambiente cambia
solo, y la primera señal suele ser una prueba que falla "sin motivo" en CI.

`AmbienteFijadoTest` lee los archivos del ambiente como texto (no necesita
Docker) y falla si:

- una imagen del compose, del Dockerfile o del devcontainer no tiene versión o
  usa `latest`;
- el PostgreSQL del compose no es **exactamente** el mismo que usan las pruebas
  ([`Imagenes.POSTGRES`](registraduria/src/test/java/edu/unisabana/tyvs/ambiente/Imagenes.java)).
  Si no coinciden, las pruebas validan un motor y el ambiente corre otro.

> Fijar la versión mayor no basta: `eclipse-temurin:17-jre` es "el último 17",
> que también cambia. Por eso el Dockerfile usa `17.0.12_7-jre-jammy`. Fijar
> todo tiene un costo —hay que actualizar a mano—, pero ese costo se paga
> cuando usted decide, no cuando la imagen cambia sola.

> **Un caso real, de este mismo curso.** Al preparar los devcontainers de los
> talleres, la etiqueta `mcr.microsoft.com/devcontainers/java:1-17-bookworm`
> traía configurado el repositorio apt de Yarn con una clave GPG que ya no era
> válida, y la feature de Docker falló al instalarse. Ningún archivo del
> repositorio había cambiado: cambió algo **de afuera** de lo que el ambiente
> dependía. La corrección está en [`.devcontainer/Dockerfile`](.devcontainer/Dockerfile),
> y la lección es que fijar versiones reduce la deriva, pero no la elimina: el
> ambiente también necesita una prueba que lo construya con regularidad (lo
> hace el workflow de CI).

**Ejercicio.** Provoque la deriva de verdad:

1. Cambie la imagen de PostgreSQL del compose a `postgres:latest` y ejecute
   `mvn test`. Lea el mensaje de `AmbienteFijadoTest`.
2. Cambie ahora la versión **solo** en `Imagenes.POSTGRES` (por ejemplo a otra
   16.x). ¿Qué prueba lo detecta y por qué es grave aunque "las dos sean 16"?
3. Busque en **otro taller del curso** una dependencia de ambiente que no esté
   fijada (una imagen, una versión de navegador, de Node, de una herramienta
   que se instala "la última"). Documéntela como defecto en `defectos.md`.

---

### Módulo 2 — Paridad de ambientes ([`ContratoDelRepositorio`](registraduria/src/test/java/edu/unisabana/tyvs/paridad/ContratoDelRepositorio.java))

En el taller de Integración vio que H2 y PostgreSQL pliegan distinto los
identificadores. Aquí se ataca el problema con una técnica: **una sola batería
de pruebas, ejecutada contra los dos motores**.

```text
ContratoDelRepositorio (abstracta: las pruebas)
 ├─ RepositorioH2Test        → H2 en memoria   (mvn test, sin Docker)
 └─ RepositorioPostgresIT    → PostgreSQL real (Testcontainers)
```

Cada subclase solo dice cómo obtener una conexión. Las dos crean el esquema con
las **mismas migraciones** de Flyway que usa la aplicación. Si una prueba pasa
en un motor y falla en el otro, encontró una diferencia entre ambientes.

El contrato ya encontró una. La consulta del reporte:

```sql
SELECT nombre FROM votante ORDER BY municipio, id
```

no dice dónde van los municipios `NULL`, y **cada motor eligió distinto**: H2
los pone primero, PostgreSQL al final. La prueba
`divergenciaDocumentadaDondeQuedanLosNulosAlOrdenar` lo deja por escrito:
cada subclase declara qué orden observa su motor.

Mientras esa prueba exista, el reporte de producción depende del ambiente, y
las pruebas que corren en su máquina (H2) **no** validan lo que verá el usuario
(PostgreSQL). Es un defecto latente que ninguna prueba unitaria podía detectar.

[`AplicacionContraPostgresIT`](registraduria/src/test/java/edu/unisabana/tyvs/paridad/AplicacionContraPostgresIT.java)
levanta la aplicación **completa** contra PostgreSQL con `@ServiceConnection`
(Spring Boot 3.1+): Spring toma la conexión del contenedor sin que usted copie
URL, usuario ni clave.

**Ejercicio.**

1. Elimine la divergencia. La prueba de divergencia debe convertirse en un
   **contrato**: un solo orden esperado, que se cumpla en los dos motores.
   Decida qué cambiar —la consulta, el esquema, la configuración de H2— y
   justifique por qué ese es el lugar correcto.
2. Encuentre **una divergencia nueva** entre H2 y PostgreSQL que no sea el
   plegado de identificadores ni el orden de los nulos. Agréguela al contrato
   como prueba que falla en un motor. (Pistas de dónde buscar: tipos, fechas,
   funciones de texto, comportamiento transaccional.)
3. H2 tiene un modo de compatibilidad con PostgreSQL. ¿Elimina las dos
   divergencias? ¿Por qué confiar en él no reemplaza a `RepositorioPostgresIT`?

---

### Módulo 3 — Ambientes efímeros y su ciclo de vida ([`ContenedorPorPruebaIT`](registraduria/src/test/java/edu/unisabana/tyvs/ciclodevida/ContenedorPorPruebaIT.java) y [`ContenedorCompartidoIT`](registraduria/src/test/java/edu/unisabana/tyvs/ciclodevida/ContenedorCompartidoIT.java))

Un ambiente **efímero** se crea para las pruebas y se destruye al terminar. No
acumula basura de corridas anteriores ni se comparte con otra persona. El
costo es crearlo.

Las dos clases hacen lo mismo (tres consultas a un PostgreSQL) con dos
estrategias:

| | Contenedor por prueba | Contenedor compartido |
|---|---|---|
| Arranques | uno por prueba | uno por clase |
| Aislamiento | total, gratis | hay que construirlo (limpiar entre pruebas) |
| Tiempo | crece con cada prueba | casi constante |

Al terminar, cada una imprime cuánto tardó en arrancar:

```text
[ciclo de vida] POR PRUEBA: 3 arranques, ... ms en total, ... ms por prueba
[ciclo de vida] COMPARTIDO: 1 arranque, ... ms en total, ... ms por prueba
```

Este módulo conecta con el taller de **Economía de las pruebas**: el arranque
de un contenedor es costo puro, que se paga en cada *commit* de cada persona.

**Ejercicio** (requiere Docker: devcontainer, Codespaces o su máquina).

1. Ejecute las dos clases tres veces y registre los tiempos. ¿Cambia la
   primera corrida respecto a las siguientes? ¿Por qué? (Piense en qué hace
   Docker la primera vez que ve una imagen.)
2. Con sus números: si el proyecto tuviera 200 pruebas contra base de datos,
   ¿cuánto costaría cada estrategia por corrida? ¿Y por semana, con 20
   *commits* diarios?
3. Implemente una tercera estrategia: **un contenedor para TODAS las clases**
   de la suite, no solo para una. Mídala. ¿Qué nuevo riesgo de aislamiento
   introduce y cómo lo controla?
4. Testcontainers tiene una opción para **reutilizar** contenedores entre
   ejecuciones. Explique por qué es razonable en su máquina y peligrosa en CI.

---

### Módulo 4 — Datos de prueba ([`MigracionesTest`](registraduria/src/test/java/edu/unisabana/tyvs/datos/MigracionesTest.java))

Tres reglas, cada una con su prueba.

**1. El esquema tiene una sola fuente: las migraciones.** Las tablas las crea
Flyway a partir de `db/migration/V1__...sql`, `V2__...sql`, en dev, en prod y
en las pruebas. Ni el repositorio ni las pruebas tienen su propio
`CREATE TABLE`. Si lo tuvieran, habría dos definiciones del esquema, y tarde o
temprano divergirían. Una migración ya aplicada **no se edita**: Flyway guarda
su *checksum* y se niega a arrancar si cambia. Los cambios van en una
migración nueva.

**2. Los datos semilla no llegan a producción.** `db/semilla/R__...sql` carga
tres votantes de ejemplo para probar a mano en dev. Solo el perfil dev la
incluye en `spring.flyway.locations`; `MigracionesTest` verifica que el perfil
prod no. Además es una migración *repetible* (`R__`), que Flyway reaplica cada
vez que cambia: por eso tiene que ser **idempotente**, y la prueba lo
comprueba ejecutándola dos veces.

**3. Las pruebas no dependen del orden.**
[`PruebaDependienteDelOrdenTest`](registraduria/src/test/java/edu/unisabana/tyvs/datos/defectuosas/PruebaDependienteDelOrdenTest.java)
está **defectuosa a propósito**. Sus dos pruebas comparten base de datos, y la
segunda necesita lo que dejó la primera. Con `@Order` fijado son verdes... en
este ambiente. Ejecute solo la segunda:

```bash
mvn test -Dtest='PruebaDependienteDelOrdenTest#segundaInscripcionDeAnaEsDuplicada'
```

```text
expected: <DUPLICATED> but was: <VALID>
```

Nadie cambió el código: cambió el **ambiente de ejecución** (qué pruebas
corren y en qué orden). Lo mismo pasa cuando un IDE ejecuta una sola prueba,
cuando CI reparte la suite entre máquinas o cuando se activa la ejecución en
paralelo.

> Las pruebas del paquete `datos.defectuosas` son **defectuosas a propósito**.
> No las copie a un proyecto real.

**Ejercicio.**

1. Arregle `PruebaDependienteDelOrdenTest` para que cada prueba pase sola, en
   cualquier orden, sin `@Order`. Explique por qué su arreglo elimina la
   dependencia en vez de esconderla.
2. Agregue una migración `V3` que cambie el esquema (por ejemplo, una columna
   nueva con un valor por defecto). No toque `V1` ni `V2`.
3. Edite a propósito `V1` después de haberla aplicado en una base persistente
   (la del compose) y levante de nuevo el ambiente. Copie el error de Flyway y
   explique por qué ese error lo protege.

---

### Módulo 5 — Configuración y secretos por ambiente ([`SecretosPorAmbienteTest`](registraduria/src/test/java/edu/unisabana/tyvs/configuracion/SecretosPorAmbienteTest.java))

Lo que cambia entre ambientes es la **configuración**, no el artefacto. El
mismo jar corre en dev (H2, datos semilla) y en prod (PostgreSQL), y lo único
que cambia es el perfil activo:

| Archivo | Qué contiene |
|---|---|
| `application.properties` | Lo común. Perfil por defecto: `dev`. |
| `application-dev.properties` | H2, datos semilla, una clave de desarrollo **pública a propósito**. |
| `application-prod.properties` | Todo lo sensible como `${VARIABLE}`, **sin valor por defecto**. |

El secreto `registraduria.clave-auditoria` protege `GET /auditoria/conteo`. Se
valida **al arrancar** (`@Validated` en
[`RegistraduriaProperties`](registraduria/src/main/java/edu/unisabana/tyvs/registry/config/RegistraduriaProperties.java)),
no en la primera petición. Eso es *fail-fast*: un despliegue mal configurado
falla delante de quien lo está haciendo, no a las tres de la mañana delante de
un usuario.

**La trampa que encontró este taller al construirse.** Si la variable de
entorno no existe, Spring Boot **no falla** al leer
`${REGISTRADURIA_CLAVE_AUDITORIA}`: entrega ese texto tal cual. Y ese texto no
está en blanco y tiene más de 16 caracteres, así que pasaba `@NotBlank` y
`@Size`. La aplicación arrancaba en producción con una "clave" que era
literalmente el nombre de la variable. Por eso `RegistraduriaProperties` tiene
además un `@Pattern` que rechaza un `${` sin resolver, y
`unPlaceholderSinResolverNoSirveComoSecreto` lo prueba.

`SecretosPorAmbienteTest` verifica además que:

- el perfil prod sin el secreto **no arranca** y el error dice qué falta;
- el perfil prod con todas sus variables **sí** arranca (el control: una
  prueba de "no arranca" que nunca arranca no demuestra nada);
- ningún `${...}` de `application-prod.properties` tiene valor por defecto;
- la clave de desarrollo no aparece en el perfil prod.

En docker compose, los secretos llegan de un archivo `.env` que **no se
versiona** (está en `.gitignore`). Se versiona `.env.ejemplo`, sin valores
reales. Y el compose usa `${VARIABLE:?mensaje}`, que hace que `docker compose`
mismo se niegue a arrancar si falta: *fail-fast* en otra capa.

**Ejercicio.**

1. Levante el compose **sin** `.env`. Copie el error. ¿En qué capa falló:
   compose o Spring?
2. Agregue un segundo secreto con la misma disciplina (validado al arrancar,
   sin valor por defecto en prod, con su prueba). Proponga cuál y para qué.
3. Busque en otro taller del curso un valor que debería depender del ambiente
   y está fijo en el código o en `application.properties`. Documente el
   riesgo.

---

### Módulo 6 — Smoke test del ambiente ([`AmbienteSmokeIT`](registraduria/src/test/java/edu/unisabana/tyvs/smoke/AmbienteSmokeIT.java) y [`SmokeAmbienteDesplegadoIT`](registraduria/src/test/java/edu/unisabana/tyvs/smoke/SmokeAmbienteDesplegadoIT.java))

Un *smoke test* no prueba reglas de negocio. Pregunta tres cosas: **¿está
vivo? ¿está listo? ¿responde lo básico?** Si la respuesta es no, no tiene
sentido correr nada más sobre ese ambiente.

Actuator distingue dos preguntas que parecen la misma:

| Endpoint | Pregunta | Si responde DOWN... |
|---|---|---|
| `/actuator/health/liveness` | ¿El proceso está sano? | Hay que reiniciarlo. |
| `/actuator/health/readiness` | ¿Puede recibir tráfico **ahora**? | Hay que esperar; no reiniciar. |

Hay dos smoke tests:

- `AmbienteSmokeIT` arranca la aplicación (perfil dev, puerto aleatorio) y la
  interroga. Corre en `mvn verify`.
- `SmokeAmbienteDesplegadoIT` **no arranca nada**: se conecta a un ambiente
  que ya está arriba. Por defecto se omite; con el compose levantado:

```bash
cd registraduria
mvn test-compile failsafe:integration-test failsafe:verify \
    -Dit.test=SmokeAmbienteDesplegadoIT -Dambiente.url=http://localhost:8080
```

Eso mismo hace el job `ambiente-desplegado` del
[workflow](.github/workflows/ambientes.yml): valida el compose, construye la
imagen, levanta el ambiente con `--wait` y le pasa el smoke test desde fuera.

En `docker-compose.yml`, `depends_on` usa `condition: service_healthy`. Sin
esa condición, `depends_on` solo ordena el **arranque**: la aplicación podría
intentar conectarse mientras PostgreSQL todavía se inicializa.

**Ejercicio.**

1. Con el compose arriba, detenga solo la base de datos
   (`docker compose stop db`) y consulte `liveness` y `readiness`. ¿Qué
   responde cada uno? ¿Es lo que debería responder una aplicación que no
   puede llegar a su base?
2. Configure el grupo de *readiness* para que refleje el estado de la base de
   datos. Repita el punto 1 y muestre el antes y el después.
3. Quite `condition: service_healthy` del compose y levante el ambiente varias
   veces desde cero (`docker compose down -v && docker compose up --build`).
   ¿Falla siempre, nunca o a veces? ¿Qué tipo de defecto es ese?

---

## PARA ENTREGAR CON ESTE TALLER

### 1) Repositorio

- `mvn clean verify` en verde tras un clon limpio, **con y sin Docker**. Sin
  Docker, las pruebas de Testcontainers deben aparecer omitidas, no fallidas.
- El workflow de GitHub Actions en verde en su repositorio (los dos jobs).

### 2) Deriva del ambiente (módulo 1)

- Evidencia de `AmbienteFijadoTest` detectando las dos derivas del ejercicio.
- Una dependencia de ambiente sin fijar encontrada en **otro taller**, en
  `defectos.md` con el formato de [`defectos_template.md`](defectos_template.md).

### 3) Paridad (módulo 2)

- La divergencia de los nulos eliminada: la prueba convertida en contrato,
  verde en H2 y en PostgreSQL, con la justificación de dónde la arregló.
- Una divergencia nueva, con la prueba que la demuestra y su análisis en
  `defectos.md`.

### 4) Ciclo de vida (módulo 3)

- La tabla de tiempos de las dos estrategias (tres corridas), la proyección a
  200 pruebas y la tercera estrategia implementada y medida.

### 5) Datos de prueba (módulo 4)

- `PruebaDependienteDelOrdenTest` arreglada, con la evidencia de que cada
  prueba pasa sola.
- La migración `V3` y el error de Flyway del ejercicio 3, explicado.

### 6) Configuración, secretos y smoke (módulos 5 y 6)

- El segundo secreto con su prueba.
- El antes y el después de *readiness* con la base de datos detenida.

### 7) Reflexión final (en el Wiki)

- ¿Cuál de los defectos que encontró en este taller habría llegado a
  producción con las pruebas de los talleres anteriores? ¿Por qué ninguna lo
  detectaba?
- Si tuviera que elegir **un solo** cambio de ambiente para su proyecto real
  (devcontainer, compose, Testcontainers, perfiles, smoke), ¿cuál sería y qué
  problema concreto resuelve?

---

## Rúbrica

| **Criterios de evaluación** | **Indicadores** | **Excelente (5 pts)** | **Bueno (4 pts)** | **Necesita mejorar (3.5 pts)** | **Deficiente (2.5 pts)** | **No cumple (0 pts)** |
|---|---|---|---|---|---|---|
| **Ejecución y ambiente como código** | Corre con y sin Docker; deriva detectada. | `mvn verify` verde con y sin Docker, workflow en verde, deriva provocada y detectada, dependencia sin fijar encontrada en otro taller. | Todo verde, deriva sin evidencia completa. | Solo corre sin Docker. | Requiere pasos no documentados. | No ejecuta. |
| **Paridad de ambientes** **(vale por 2)** | Divergencias reales entre motores. | Divergencia de los nulos convertida en contrato y justificada; una divergencia nueva demostrada con prueba en los dos motores. | Nulos resueltos, divergencia nueva sin prueba en PostgreSQL. | Solo resuelve los nulos. | Cambia la prueba para que pase sin entender la causa. | No aborda. |
| **Ciclo de vida y costo** | Decisión con números. | Tiempos medidos en tres corridas, proyección razonada, tercera estrategia implementada y su riesgo controlado. | Tiempos y proyección, sin tercera estrategia. | Tiempos sin análisis. | Números inventados o de una sola corrida. | No mide. |
| **Datos de prueba y aislamiento** **(vale por 2)** | Esquema, semilla y orden. | Prueba dependiente arreglada en su causa, cada prueba pasa sola; V3 sin tocar V1/V2; error de checksum explicado. | Arreglo correcto con análisis parcial. | La "arregla" fijando el orden o fusionando pruebas. | Edita migraciones aplicadas. | No aborda. |
| **Configuración, secretos y smoke** | Fail-fast y readiness. | Segundo secreto validado al arrancar y probado; readiness refleja la base de datos con evidencia antes/después. | Uno de los dos completo. | Secreto sin prueba o readiness sin evidencia. | Secreto con valor por defecto en prod. | No aborda. |
| **Registro de defectos** | `defectos.md` con defectos de ambiente. | Cada defecto dice dónde falla, dónde no y qué diferencia lo explica, con reproducción exacta. | Defectos correctos con reproducción incompleta. | Defectos sin ambiente identificado. | Defectos de lógica, no de ambiente. | No entrega. |
| **Documentación y reflexión** | Wiki con análisis. | Reflexión que conecta los defectos encontrados con lo que las pruebas anteriores no podían ver, con un cambio concreto propuesto. | Clara pero sin profundidad. | Incompleta. | Mínima. | Sin documentación. |

> **Cómo suma**: 9 criterios × 5 pts = **45 puntos**. Son 7 filas, pero
> *Paridad de ambientes* y *Datos de prueba y aislamiento* valen por dos cada
> una — son el núcleo del taller.

| Rango de puntaje | Desempeño |
| ---------------- | --------- |
| 41 – 45 | Excelente dominio técnico y metodológico. |
| 32 – 40 | Buen trabajo con documentación o análisis parcial. |
| 27 – 31 | Cumple con lo básico pero sin profundidad. |
| < 27 | No cumple con los criterios mínimos del taller. |

---

## Créditos y uso académico

Material del curso **Testing y Validación de Software**, Maestría en
Ingeniería de Software, **Universidad de La Sabana**.

Las pruebas del paquete `datos.defectuosas` son **defectuosas a propósito**.
No las copie a un proyecto real. Las claves de `application-dev.properties`,
`.env.ejemplo` y del workflow son de ejemplo y no protegen nada real.

---

### Licencia de uso

Este material se distribuye bajo la licencia [Creative Commons Atribución-NoComercial-CompartirIgual 4.0 Internacional (CC BY-NC-SA 4.0)](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.es).

Puedes **usar, adaptar o compartir** este contenido con fines educativos, siempre que:

1. Se reconozca la autoría del profesor **César Augusto Vega Fernández**.
2. No se utilice con fines comerciales.
3. Las obras derivadas se distribuyan bajo la misma licencia.
