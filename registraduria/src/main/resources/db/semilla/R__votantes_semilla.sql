-- Datos semilla, SOLO para el perfil dev (ver application-dev.properties).
--
-- Es una migracion "repetible" (prefijo R__): Flyway la vuelve a aplicar cada
-- vez que cambia su contenido. Por eso tiene que ser idempotente: primero borra
-- su propio rango de ids y luego inserta. Los ids 9000-9099 quedan reservados
-- para la semilla; las pruebas y las personas reales usan otros.
DELETE FROM votante WHERE id BETWEEN 9000 AND 9099;

INSERT INTO votante (id, nombre, edad, vivo, municipio) VALUES (9001, 'Semilla Chia',    34, TRUE, 'Chia');
INSERT INTO votante (id, nombre, edad, vivo, municipio) VALUES (9002, 'Semilla Cajica',  51, TRUE, 'Cajica');
INSERT INTO votante (id, nombre, edad, vivo, municipio) VALUES (9003, 'Semilla Sin Dato', 27, TRUE, NULL);
