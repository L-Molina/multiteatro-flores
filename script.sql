-- =====================================================================
--  Multiteatro Flores — Esquema de base de datos y datos de ejemplo
--  Motor: MySQL 8 / 9 (InnoDB, utf8mb4)
--
--  Uso:
--      mysql -u root -e "CREATE DATABASE multiteatro_db;"
--      mysql -u root multiteatro_db < script.sql
--
--  ATENCION: el script empieza borrando las tablas para poder correrlo
--  varias veces y dejar siempre la misma base de demostracion.
--
--  NOTA: la app tiene spring.jpa.hibernate.ddl-auto=none, es decir que
--  Hibernate NO crea ni modifica el esquema. Este archivo es la unica
--  fuente de verdad de la estructura de la base.
-- =====================================================================

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS entrada;
DROP TABLE IF EXISTS compra;
DROP TABLE IF EXISTS funcion;
DROP TABLE IF EXISTS asiento;
DROP TABLE IF EXISTS sala;
DROP TABLE IF EXISTS tipo_asiento;
DROP TABLE IF EXISTS obra;
DROP TABLE IF EXISTS teatro;
DROP TABLE IF EXISTS usuario;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- usuario — personas que usan el sistema. El rol define que pueden hacer.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- hash BCrypt, nunca la contrasena en claro
    rol           ENUM('ADMIN','CLIENTE','SUPERADMIN') NOT NULL,
    creado_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY email (email)                      -- el login identifica al usuario
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- teatro — el complejo teatral. Cada uno tiene un administrador (ADMIN).
-- admin_usuario_id es UNIQUE: un ADMIN administra como maximo un teatro.
-- ---------------------------------------------------------------------
CREATE TABLE teatro (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    admin_usuario_id INT DEFAULT NULL,            -- NULL si todavia no tiene administrador
    nombre           VARCHAR(100) NOT NULL,
    direccion        VARCHAR(255) NOT NULL,
    UNIQUE KEY admin_usuario_id (admin_usuario_id),
    CONSTRAINT fk_teatro_admin FOREIGN KEY (admin_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- sala — salas fisicas dentro de un teatro.
-- ---------------------------------------------------------------------
CREATE TABLE sala (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    teatro_id INT NOT NULL,
    nombre    VARCHAR(50) NOT NULL,
    capacidad INT NOT NULL,
    KEY fk_sala_teatro (teatro_id),
    CONSTRAINT fk_sala_teatro FOREIGN KEY (teatro_id) REFERENCES teatro (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- tipo_asiento — categoria de butaca y su multiplicador sobre el precio
-- base de la funcion.
--   Normal = 1.0
--   VIP    = 1.3  (30% mas)
-- ---------------------------------------------------------------------
CREATE TABLE tipo_asiento (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(50) NOT NULL,
    multiplicador  DOUBLE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- obra — todas las obras comparten esta tabla y se separan por tipo.
-- El precio es el precio base de la entrada (sin multiplicador de butaca).
-- ---------------------------------------------------------------------
CREATE TABLE obra (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    titulo           VARCHAR(150) NOT NULL,
    tipo             ENUM('TEATRAL','STAND_UP','CONCIERTO') NOT NULL,
    precio           DECIMAL(10,2) NOT NULL,
    clasificacion    VARCHAR(20)  NOT NULL,       -- ej. 'ATP', '+13', '+18'
    descripcion      VARCHAR(1000) DEFAULT NULL,
    duracion_minutos INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- asiento — butaca fisica de una sala, con su fila y numero.
-- ---------------------------------------------------------------------
CREATE TABLE asiento (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    sala_id         INT NOT NULL,
    tipo_asiento_id INT NOT NULL,
    fila            VARCHAR(10) DEFAULT NULL,
    numero          INT NOT NULL,
    UNIQUE KEY uq_asiento_ubicacion (sala_id, fila, numero),  -- no se repite butaca en la misma sala
    KEY fk_asiento_tipo (tipo_asiento_id),
    CONSTRAINT fk_asiento_sala FOREIGN KEY (sala_id)        REFERENCES sala (id),
    CONSTRAINT fk_asiento_tipo FOREIGN KEY (tipo_asiento_id) REFERENCES tipo_asiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- funcion — una puesta de una obra en una sala, en una fecha y hora.
-- El precio se hereda de la obra al crearla, pero queda como snapshot:
-- cambiar el precio de la obra no altera funciones ya programadas.
-- ---------------------------------------------------------------------
CREATE TABLE funcion (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    obra_id    INT NOT NULL,
    sala_id    INT NOT NULL,
    fecha_hora DATETIME NOT NULL,
    precio     DOUBLE NOT NULL,
    KEY fk_funcion_obra (obra_id),
    KEY fk_funcion_sala (sala_id),
    CONSTRAINT fk_funcion_obra FOREIGN KEY (obra_id) REFERENCES obra (id),
    CONSTRAINT fk_funcion_sala FOREIGN KEY (sala_id) REFERENCES sala (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- compra — la operacion de comprar. Una compra puede tener N entradas.
-- ---------------------------------------------------------------------
CREATE TABLE compra (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT NOT NULL,
    fecha       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
    total       DECIMAL(10,2) NOT NULL,
    monto_total DOUBLE NOT NULL,
    KEY fk_compra_usuario (usuario_id),
    CONSTRAINT fk_compra_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- entrada — el ticket individual. Se genera al comprar (no se crea por API).
--
-- TRUCO CLAVE DEL MODELO — la columna generada asiento_si_activa:
--   asiento_si_activa = CASE WHEN estado = 'ACTIVA' THEN asiento_id ELSE NULL END
-- Con MySQL los NULL no cuentan como duplicados en un indice UNIQUE, asi que
-- el indice unico (funcion_id, asiento_si_activa) permite muchas entradas
-- CANCELADAS del mismo asiento, pero impide DOS entradas ACTIVAS para el
-- mismo asiento en la misma funcion.
-- ---------------------------------------------------------------------
CREATE TABLE entrada (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    compra_id         INT NOT NULL,
    funcion_id        INT NOT NULL,
    asiento_id        INT NOT NULL,
    precio_pagado     DECIMAL(10,2) NOT NULL,
    estado            VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    asiento_si_activa INT GENERATED ALWAYS AS
                      (CASE WHEN estado = 'ACTIVA' THEN asiento_id ELSE NULL END) STORED,
    codigo            VARCHAR(50) NOT NULL,
    UNIQUE KEY uq_entrada_funcion_asiento_activa (funcion_id, asiento_si_activa),
    KEY fk_entrada_compra (compra_id),
    KEY fk_entrada_asiento (asiento_id),
    CONSTRAINT fk_entrada_compra  FOREIGN KEY (compra_id)  REFERENCES compra (id),
    CONSTRAINT fk_entrada_funcion FOREIGN KEY (funcion_id) REFERENCES funcion (id),
    CONSTRAINT fk_entrada_asiento FOREIGN KEY (asiento_id) REFERENCES asiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- ticket — comprobante de venta, UNO POR CADA BUTACA (1:1 con entrada).
-- Guarda un snapshot de los datos comerciales.
-- ---------------------------------------------------------------------
CREATE TABLE ticket (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    codigo               VARCHAR(50)  NOT NULL,
    fecha_emision        DATETIME     NOT NULL,
    obra_titulo          VARCHAR(150) NOT NULL,
    obra_clasificacion   VARCHAR(20),
    funcion_fecha_hora   DATETIME     NOT NULL,
    teatro_nombre        VARCHAR(100) NOT NULL,
    sala_nombre          VARCHAR(50)  NOT NULL,
    butaca_fila          VARCHAR(10),
    butaca_numero        INT,
    precio_pagado        DECIMAL(10,2) NOT NULL,
    entrada_id           BIGINT,
    UNIQUE KEY uq_ticket_codigo (codigo),
    UNIQUE KEY uq_ticket_entrada (entrada_id),
    CONSTRAINT fk_ticket_entrada FOREIGN KEY (entrada_id) REFERENCES entrada(id)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
--  DATOS DE EJEMPLO
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios.
--
--   superadmin@multiteatroflores.com -> superadmin123
--   claudia@noblesbestias.com         -> admin123
--   daniel@otromundo.com              -> admin123
--   luis@teatrohorizonte.com          -> admin123
--   gabriela@mandorla.com             -> admin123
--   brenda@teatroenlabruma.com        -> admin123
--   damian@teatrourbano.com           -> admin123
--   usuario1@example.com              -> cliente123
--   usuario2@example.com              -> cliente123
--   usuario3@example.com              -> cliente123
-- ---------------------------------------------------------------------
INSERT INTO usuario (id, nombre, email, password_hash, rol) VALUES
  (1, 'Lautaro',                'superadmin@multiteatroflores.com', '$2a$10$K85LGlwq962vVHhIYaVU6eY10YLNyChpk7r/Y9idOBh1AyJNfQHpG', 'SUPERADMIN'),
  (2, 'Claudia Eichenberg',     'claudia@noblesbestias.com',         '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (3, 'Daniel Gandara',         'daniel@otromundo.com',              '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (4, 'Luis Enrique Cornejo',   'luis@teatrohorizonte.com',          '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (5, 'Gabriela Pagés',         'gabriela@mandorla.com',             '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (6, 'Brenda Rusconi',         'brenda@teatroenlabruma.com',        '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (7, 'Damián Dominguez',       'damian@teatrourbano.com',           '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (8, 'Usuario1',               'usuario1@example.com',              '$2a$10$mPfAWjHUQfw38H9qAYtWbOEtEIhWTrHBZhsvIpQIjGaG7QHZLoMta', 'CLIENTE'),
  (9, 'Usuario2',               'usuario2@example.com',              '$2a$10$mPfAWjHUQfw38H9qAYtWbOEtEIhWTrHBZhsvIpQIjGaG7QHZLoMta', 'CLIENTE'),
  (10, 'Usuario3',              'usuario3@example.com',              '$2a$10$mPfAWjHUQfw38H9qAYtWbOEtEIhWTrHBZhsvIpQIjGaG7QHZLoMta', 'CLIENTE');

-- ---------------------------------------------------------------------
-- Teatros. Cada ADMIN administra exactamente un teatro.
-- ---------------------------------------------------------------------
INSERT INTO teatro (id, admin_usuario_id, nombre, direccion) VALUES
  (1, 2, 'Teatro de Las Nobles Bestias', 'Av. Rivadavia 1234, CABA'),
  (2, 3, 'Otromundo Teatro',             'Av. Corrientes 2345, CABA'),
  (3, 4, 'Teatro Horizonte',             'Av. Santa Fe 3456, CABA'),
  (4, 5, 'Mandorla Teatro',              'Av. Belgrano 4567, CABA'),
  (5, 6, 'Teatro en la Bruma',           'Av. Callao 5678, CABA'),
  (6, 7, 'Teatro Urbano',                'Av. San Juan 6789, CABA');

-- ---------------------------------------------------------------------
-- Salas: 3 por teatro. IDs 1-3 teatro 1, 4-6 teatro 2, etc.
-- ---------------------------------------------------------------------
INSERT INTO sala (id, teatro_id, nombre, capacidad) VALUES
  (1, 1, 'Sala 1', 25), (2, 1, 'Sala 2', 25), (3, 1, 'Sala 3', 25),
  (4, 2, 'Sala 1', 25), (5, 2, 'Sala 2', 25), (6, 2, 'Sala 3', 25),
  (7, 3, 'Sala 1', 25), (8, 3, 'Sala 2', 25), (9, 3, 'Sala 3', 25),
  (10, 4, 'Sala 1', 25), (11, 4, 'Sala 2', 25), (12, 4, 'Sala 3', 25),
  (13, 5, 'Sala 1', 25), (14, 5, 'Sala 2', 25), (15, 5, 'Sala 3', 25),
  (16, 6, 'Sala 1', 25), (17, 6, 'Sala 2', 25), (18, 6, 'Sala 3', 25);

-- ---------------------------------------------------------------------
-- Tipos de asiento: Normal sin recargo, VIP +30%.
-- ---------------------------------------------------------------------
INSERT INTO tipo_asiento (id, nombre, multiplicador) VALUES
  (1, 'Normal', 1.0),
  (2, 'VIP',    1.3);

-- ---------------------------------------------------------------------
-- Obras: 10 en total, separadas por tipo.
--   TEATRAL   -> $30.000
--   STAND_UP  -> $20.000
--   CONCIERTO -> $12.500
-- ---------------------------------------------------------------------
INSERT INTO obra (id, titulo, tipo, precio, clasificacion, descripcion, duracion_minutos) VALUES
  (1,  'Toc Toc',                      'TEATRAL',   30000.00, 'ATP', 'Comedia de Laurent Baffie sobre un grupo de pacientes con TOC en la sala de espera de un psiquiatra.', 90),
  (2,  'La Función Que Sale Mal',      'TEATRAL',   30000.00, '+13', 'Comedia de enredos donde nada sale como estaba previsto y el público forma parte del desastre.', 110),
  (3,  'Odisea de Florencio Varela',   'TEATRAL',   30000.00, '+13', 'Aventura teatral inspirada en el viaje de Ulises, trasladada al conurbano bonaerense.', 100),
  (4,  'Barrio Privado',               'TEATRAL',   30000.00, '+16', 'Drama intenso sobre vínculos, deudas y el peso del pasado en un pueblo del interior.', 105),
  (5,  'Pablo Picotto',                'STAND_UP',  20000.00, '+18', 'Stand-up de observación sobre la vida cotidiana, la familia y los pequeños fracasos.', 75),
  (6,  'Julian Bellese',               'STAND_UP',  20000.00, '+18', 'Stand-up con historias personales, redes sociales y anécdotas de gira.', 70),
  (7,  'Turco Naim',                   'STAND_UP',  20000.00, '+18', 'Stand-up sobre costumbres, identidad y convivencia en Buenos Aires.', 75),
  (8,  'Jammin',                       'CONCIERTO', 12500.00, 'ATP', 'Concierto de reggae y fusión en formato banda completa.', 120),
  (9,  'Niño Salvaje',                 'CONCIERTO', 12500.00, 'ATP', 'Show de rock alternativo con puesta en escena audiovisual.', 100),
  (10, 'Experiencia Beatle',           'CONCIERTO', 12500.00, 'ATP', 'Tributo sinfónico al repertorio de The Beatles.', 110);

-- ---------------------------------------------------------------------
-- Asientos: cada una de las 18 salas tiene 25 butacas (5x5).
-- Filas A-E, columnas 1-5. Fila A = 5 VIP; filas B-E = 20 Normales.
-- IDs resultantes: sala 1 -> 1-25, sala 2 -> 26-50, ..., sala 18 -> 426-450.
-- ---------------------------------------------------------------------
INSERT INTO asiento (sala_id, tipo_asiento_id, fila, numero)
SELECT s.id,
       CASE WHEN f.fila = 'A' THEN 2 ELSE 1 END,
       f.fila,
       c.numero
FROM sala s
CROSS JOIN (
    SELECT 'A' AS fila, 1 AS orden
    UNION ALL SELECT 'B', 2
    UNION ALL SELECT 'C', 3
    UNION ALL SELECT 'D', 4
    UNION ALL SELECT 'E', 5
) f
CROSS JOIN (
    SELECT 1 AS numero
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) c
ORDER BY s.id, f.orden, c.numero;

-- ---------------------------------------------------------------------
-- Funciones: dos por cada sala (36 en total), con obras de los tres tipos.
-- El precio de cada función coincide con el precio base de su obra.
-- ---------------------------------------------------------------------
INSERT INTO funcion (id, obra_id, sala_id, fecha_hora, precio) VALUES
  -- Teatro 1
  (1,  1,  1, '2026-11-06 20:00:00', 30000.00),
  (2,  5,  1, '2026-11-07 21:00:00', 20000.00),
  (3,  2,  2, '2026-11-06 20:30:00', 30000.00),
  (4,  8,  2, '2026-11-08 21:00:00', 12500.00),
  (5,  3,  3, '2026-11-07 20:00:00', 30000.00),
  (6,  10, 3, '2026-11-09 20:00:00', 12500.00),
  -- Teatro 2
  (7,  4,  4, '2026-11-13 20:00:00', 30000.00),
  (8,  6,  4, '2026-11-14 21:00:00', 20000.00),
  (9,  9,  5, '2026-11-13 21:00:00', 12500.00),
  (10, 1,  5, '2026-11-15 20:00:00', 30000.00),
  (11, 2,  6, '2026-11-14 20:00:00', 30000.00),
  (12, 7,  6, '2026-11-16 21:00:00', 20000.00),
  -- Teatro 3
  (13, 3,  7, '2026-11-20 20:00:00', 30000.00),
  (14, 10, 7, '2026-11-21 21:00:00', 12500.00),
  (15, 4,  8, '2026-11-20 21:00:00', 30000.00),
  (16, 5,  8, '2026-11-22 21:00:00', 20000.00),
  (17, 1,  9, '2026-11-21 20:00:00', 30000.00),
  (18, 8,  9, '2026-11-23 20:00:00', 12500.00),
  -- Teatro 4
  (19, 2,  10, '2026-11-27 20:00:00', 30000.00),
  (20, 6,  10, '2026-11-28 21:00:00', 20000.00),
  (21, 3,  11, '2026-11-27 21:00:00', 30000.00),
  (22, 9,  11, '2026-11-29 21:00:00', 12500.00),
  (23, 4,  12, '2026-11-28 20:00:00', 30000.00),
  (24, 7,  12, '2026-11-30 21:00:00', 20000.00),
  -- Teatro 5
  (25, 1,  13, '2026-12-04 20:00:00', 30000.00),
  (26, 5,  13, '2026-12-05 21:00:00', 20000.00),
  (27, 2,  14, '2026-12-04 21:00:00', 30000.00),
  (28, 8,  14, '2026-12-06 20:00:00', 12500.00),
  (29, 3,  15, '2026-12-05 20:00:00', 30000.00),
  (30, 10, 15, '2026-12-07 20:00:00', 12500.00),
  -- Teatro 6
  (31, 4,  16, '2026-12-11 20:00:00', 30000.00),
  (32, 6,  16, '2026-12-12 21:00:00', 20000.00),
  (33, 9,  17, '2026-12-11 21:00:00', 12500.00),
  (34, 1,  17, '2026-12-13 20:00:00', 30000.00),
  (35, 2,  18, '2026-12-12 20:00:00', 30000.00),
  (36, 7,  18, '2026-12-13 21:00:00', 20000.00);

-- ---------------------------------------------------------------------
-- Compras de ejemplo.
--
-- Usuario1 (id 8): 2 entradas para Toc Toc (función 1, sala 1).
-- Usuario2 (id 9): 1 entrada para Pablo Picotto (función 2, sala 1).
-- Usuario3 (id 10): 4 entradas para Jammin (función 8, sala 2),
--                   una de ellas VIP para mostrar el recargo del 30%.
-- ---------------------------------------------------------------------
INSERT INTO compra (id, usuario_id, fecha, estado, total, monto_total) VALUES
  (1, 8,  '2026-10-20 18:30:00', 'CONFIRMADA', 60000.00, 60000.0),
  (2, 9,  '2026-10-21 12:10:00', 'CONFIRMADA', 20000.00, 20000.0),
  (3, 10, '2026-10-22 09:45:00', 'CONFIRMADA', 53750.00, 53750.0);

-- Asientos de la sala 1: B1=6, B2=7, C3=13.
-- Asientos de la sala 2: A2=27 (VIP), D1=41, D2=42, D3=43.
INSERT INTO entrada (id, compra_id, funcion_id, asiento_id, precio_pagado, estado, codigo) VALUES
  (1, 1, 1, 6,  30000.00, 'ACTIVA', 'E1A2B3C4D5F6'),
  (2, 1, 1, 7,  30000.00, 'ACTIVA', 'E2A2B3C4D5F6'),
  (3, 2, 2, 13, 20000.00, 'ACTIVA', 'E3A2B3C4D5F6'),
  (4, 3, 8, 27, 16250.00, 'ACTIVA', 'E4A2B3C4D5F6'),
  (5, 3, 8, 41, 12500.00, 'ACTIVA', 'E5A2B3C4D5F6'),
  (6, 3, 8, 42, 12500.00, 'ACTIVA', 'E6A2B3C4D5F6'),
  (7, 3, 8, 43, 12500.00, 'ACTIVA', 'E7A2B3C4D5F6');

-- Tickets: un comprobante por cada entrada, con snapshot de los datos.
INSERT INTO ticket (id, codigo, fecha_emision, obra_titulo, obra_clasificacion,
                    funcion_fecha_hora, teatro_nombre, sala_nombre,
                    butaca_fila, butaca_numero, precio_pagado, entrada_id) VALUES
  (1, 'TCK-EJEMPLO0001', '2026-10-20 18:30:00', 'Toc Toc', 'ATP',
   '2026-11-06 20:00:00', 'Teatro de Las Nobles Bestias', 'Sala 1', 'B', 1, 30000.00, 1),
  (2, 'TCK-EJEMPLO0002', '2026-10-20 18:30:00', 'Toc Toc', 'ATP',
   '2026-11-06 20:00:00', 'Teatro de Las Nobles Bestias', 'Sala 1', 'B', 2, 30000.00, 2),
  (3, 'TCK-EJEMPLO0003', '2026-10-21 12:10:00', 'Pablo Picotto', '+18',
   '2026-11-07 21:00:00', 'Teatro de Las Nobles Bestias', 'Sala 1', 'C', 3, 20000.00, 3),
  (4, 'TCK-EJEMPLO0004', '2026-10-22 09:45:00', 'Jammin', 'ATP',
   '2026-11-08 21:00:00', 'Teatro de Las Nobles Bestias', 'Sala 2', 'A', 2, 16250.00, 4),
  (5, 'TCK-EJEMPLO0005', '2026-10-22 09:45:00', 'Jammin', 'ATP',
   '2026-11-08 21:00:00', 'Teatro de Las Nobles Bestias', 'Sala 2', 'D', 1, 12500.00, 5),
  (6, 'TCK-EJEMPLO0006', '2026-10-22 09:45:00', 'Jammin', 'ATP',
   '2026-11-08 21:00:00', 'Teatro de Las Nobles Bestias', 'Sala 2', 'D', 2, 12500.00, 6),
  (7, 'TCK-EJEMPLO0007', '2026-10-22 09:45:00', 'Jammin', 'ATP',
   '2026-11-08 21:00:00', 'Teatro de Las Nobles Bestias', 'Sala 2', 'D', 3, 12500.00, 7);
