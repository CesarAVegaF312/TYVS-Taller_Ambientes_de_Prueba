-- Migracion 1. Una vez aplicada en cualquier ambiente, este archivo NO se
-- edita: Flyway guarda su checksum y se niega a arrancar si cambia. Los
-- cambios de esquema van en una migracion nueva (V3, V4...).
CREATE TABLE votante (
    id     INTEGER      PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    edad   INTEGER      NOT NULL,
    vivo   BOOLEAN      NOT NULL
);
