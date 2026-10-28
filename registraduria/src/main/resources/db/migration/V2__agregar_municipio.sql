-- Migracion 2: el esquema evoluciona sin tocar la V1.
-- El municipio es opcional: los registros anteriores quedan con NULL.
ALTER TABLE votante ADD COLUMN municipio VARCHAR(80);
