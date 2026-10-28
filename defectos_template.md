# Registro de Defectos DE AMBIENTE (plantilla)

En este taller el defecto casi nunca esta en una linea de Java. Esta en la
distancia entre dos ambientes: lo que funciona en su maquina y no en CI, lo que
pasa contra H2 y falla contra PostgreSQL, la prueba que solo es verde si corre
despues de otra.

Un defecto de ambiente se documenta distinto: lo importante no es la entrada y
la salida, sino **en que ambiente aparece y en cual no**.

---

## Formato

### Defecto XX — <titulo corto>

- **Ambiente donde falla**: (CI, devcontainer, compose, Testcontainers, maquina
  local con Windows...) con versiones concretas.
- **Ambiente donde NO falla**: idem. Si no tiene un ambiente donde pase, no es
  un defecto de ambiente.
- **Diferencia que lo explica**: version de imagen, motor de base de datos,
  orden de ejecucion, variable de entorno, zona horaria, sistema operativo...
- **Como reproducirlo**: comandos exactos, desde un clon limpio.
- **Prueba que lo detecta**: clase y metodo. Si ninguna prueba lo detecta,
  digalo: es el hallazgo mas importante.
- **Correccion**: que se cambio y por que eso elimina la diferencia en vez de
  esconderla.
- **Estado**: Abierto | En progreso | Resuelto

---

## Tabla resumen

| ID | Falla en | No falla en | Diferencia | Prueba que lo detecta | Estado |
|----|----------|-------------|------------|-----------------------|--------|
| XX | ...      | ...         | ...        | ...                   | Abierto |
