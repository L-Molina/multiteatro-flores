-- =====================================================================
--  Multiteatro Flores — Esquema de base de datos y datos de ejemplo
--  Motor: MySQL 8 / 9 (InnoDB, utf8mb4)
--
--  Uso:
--      mysql -u root -e "CREATE DATABASE multiteatro_db;"
--      mysql -u root multiteatro_db < script.sql
--
--  NOTA: la app tiene spring.jpa.hibernate.ddl-auto=none, es decir que
--  Hibernate NO crea ni modifica el esquema. Este archivo es la única
--  fuente de verdad de la estructura de la base.
-- =====================================================================

-- ---------------------------------------------------------------------
-- usuario — personas que usan el sistema. El rol define qué pueden hacer.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- hash BCrypt, nunca la contraseña en claro
    rol           ENUM('ADMIN','CLIENTE','SUPERADMIN') NOT NULL,
    creado_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY email (email)                      -- el login identifica al usuario
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- teatro — el complejo teatral. Cada uno tiene un administrador (ADMIN).
-- admin_usuario_id es UNIQUE: un ADMIN administra como máximo un teatro.
-- ---------------------------------------------------------------------
CREATE TABLE teatro (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    admin_usuario_id INT DEFAULT NULL,            -- NULL si todavia no tiene administrador
    nombre          VARCHAR(100) NOT NULL,
    direccion       VARCHAR(255) NOT NULL,
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
-- tipo_asiento — categorias de asiento y su precio. Es una entidad
-- (no un enum) para poder agregar tipos sin tocar el codigo.
-- ---------------------------------------------------------------------
CREATE TABLE tipo_asiento (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio DOUBLE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- asiento — butaca fisica de una sala, con su fila y numero.
-- El precio NO esta en el asiento: sale del tipo_asiento (se reutiliza).
-- ---------------------------------------------------------------------
CREATE TABLE asiento (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    sala_id        INT NOT NULL,
    tipo_asiento_id INT NOT NULL,
    fila           VARCHAR(10) DEFAULT NULL,
    numero         INT NOT NULL,
    UNIQUE KEY uq_asiento_ubicacion (sala_id, fila, numero),  -- no se repite butaca en la misma sala
    KEY fk_asiento_tipo (tipo_asiento_id),
    CONSTRAINT fk_asiento_sala  FOREIGN KEY (sala_id)        REFERENCES sala (id),
    CONSTRAINT fk_asiento_tipo  FOREIGN KEY (tipo_asiento_id) REFERENCES tipo_asiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- obra — la obra de teatro. clasificacion es texto libre (ATP, +13, +18).
-- ---------------------------------------------------------------------
CREATE TABLE obra (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    titulo           VARCHAR(150) NOT NULL,
    clasificacion    VARCHAR(20)  NOT NULL,      -- ej. 'ATP', '+13', '+18'
    descripcion      VARCHAR(1000) DEFAULT NULL,
    duracion_minutos INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- funcion — una puesta de una obra en una sala, en una fecha y hora.
-- ---------------------------------------------------------------------
CREATE TABLE funcion (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    obra_id    INT NOT NULL,
    sala_id    INT NOT NULL,
    fecha_hora DATETIME NOT NULL,
    precio     DOUBLE NOT NULL,                  -- precio base de la funcion
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
    total       DECIMAL(10,2) NOT NULL,          -- importe exacto (BigDecimal en Java)
    monto_total DOUBLE NOT NULL,                 -- mismo importe como double, para el frontend
    KEY fk_compra_usuario (usuario_id),
    CONSTRAINT fk_compra_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- entrada — el ticket individual. Se genera al comprar (no se crea por API).
--
-- TRUCO CLAVE DEL MODELO — la columna generada asiento_si_activa:
--   asiento_si_activa = CASE WHEN estado = 'ACTIVA' THEN asiento_id ELSE NULL END
-- Con MySQL los NULL no cuentan como duplicados en un indice UNIQUE, asi que
-- el indice unico (funcion_id, asiento_si_activa) permite manyas entradas
-- CANCELADAS del mismo asiento, pero impide DOS entradas ACTIVAS para el
-- mismo asiento en la misma funcion. Asi se garantiza la exclusividad del
-- asiento a nivel de base de datos, no solo en el codigo Java, y dos
-- compras simultaneas no pueden vender la misma butaca.
-- ---------------------------------------------------------------------
CREATE TABLE entrada (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    compra_id       INT NOT NULL,
    funcion_id      INT NOT NULL,
    asiento_id      INT NOT NULL,
    precio_pagado   DECIMAL(10,2) NOT NULL,      -- precio congelado al momento de comprar
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    asiento_si_activa INT GENERATED ALWAYS AS
                      (CASE WHEN estado = 'ACTIVA' THEN asiento_id ELSE NULL END) STORED,
    codigo          VARCHAR(50) NOT NULL,         -- codigo corto de 12 chars para el ticket
    UNIQUE KEY uq_entrada_funcion_asiento_activa (funcion_id, asiento_si_activa),
    KEY fk_entrada_compra (compra_id),
    KEY fk_entrada_asiento (asiento_id),
    CONSTRAINT fk_entrada_compra  FOREIGN KEY (compra_id)  REFERENCES compra (id),
    CONSTRAINT fk_entrada_funcion FOREIGN KEY (funcion_id) REFERENCES funcion (id),
    CONSTRAINT fk_entrada_asiento FOREIGN KEY (asiento_id) REFERENCES asiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
--  DATOS DE EJEMPLO
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios. Los password_hash son BCrypt de las contrasenas que figuran
-- al lado (no se puede "descifrar" el hash, se compara con matches()).
--
--   superadmin@multiteatroflores.com -> superadmin123
--   marta@teatrolanus.com             -> admin123
--   carlos@teatrobanfield.com        -> admin123
--   lautaro@example.com               -> cliente123
--   ana@example.com                  -> cliente123
-- ---------------------------------------------------------------------
INSERT INTO usuario (id, nombre, email, password_hash, rol) VALUES
  (1, 'Root Admin',    'superadmin@multiteatroflores.com', '$2a$10$K85LGlwq962vVHhIYaVU6eY10YLNyChpk7r/Y9idOBh1AyJNfQHpG', 'SUPERADMIN'),
  (2, 'Marta Gimenez', 'marta@teatrolanus.com',             '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (3, 'Carlos Suarez', 'carlos@teatrobanfield.com',        '$2a$10$Bc.IaiU8xPoeEBThk6RGMuUC0FLKqAHlJ.WUTv35ffzZfqIPbstOu', 'ADMIN'),
  (4, 'Lautaro Molina','lautaro@example.com',               '$2a$10$mPfAWjHUQfw38H9qAYtWbOEtEIhWTrHBZhsvIpQIjGaG7QHZLoMta', 'CLIENTE'),
  (5, 'Ana Perez',     'ana@example.com',                    '$2a$10$tm3HjToUjNYWWt8gChmhMuHW9eaaibPXAgYe9hcQzrIoZbGW7/3Rq', 'CLIENTE');

-- ---------------------------------------------------------------------
-- Teatros. id 4, 5 y 6 quedan sin administrador a proposito, para probar
-- que un ADMIN no puede administrarlos (403 "No administrás este teatro").
-- ---------------------------------------------------------------------
INSERT INTO teatro (id, admin_usuario_id, nombre, direccion) VALUES
  (1, 2, 'Teatro Lanús',    'Av. Hipólito Yrigoyen 3200, Lanús'),
  (2, 3, 'Teatro Banfield', 'Córdoba 1234, Banfield'),
  (4, NULL, 'Teatro Flores 2',  'Av. Corrientes 1500, CABA'),
  (5, NULL, 'Teatro Prueba',    'Calle Falsa 123, CABA'),
  (6, NULL, 'Teatro Postman',   'Calle Demo 456, CABA');

-- ---------------------------------------------------------------------
-- Salas
-- ---------------------------------------------------------------------
INSERT INTO sala (id, teatro_id, nombre, capacidad) VALUES
  (1, 1, 'Sala Principal', 12),
  (2, 2, 'Sala Intima',     8);

-- ---------------------------------------------------------------------
-- Tipos de asiento
-- ---------------------------------------------------------------------
INSERT INTO tipo_asiento (id, nombre, precio) VALUES
  (1, 'Standard', 5000.0),
  (2, 'VIP',      8500.0);

-- ---------------------------------------------------------------------
-- Obras
-- ---------------------------------------------------------------------
INSERT INTO obra (id, titulo, clasificacion, descripcion, duracion_minutos) VALUES
  (1, 'El Enfermo Imaginario', 'ATP', 'Comedia de Jean Anouilh donde un enfermo imaginario se debate entre la realidad y la locura.', 95),
  (2, 'Un Tranvía Llamado Deseo', '+13', 'Drama de Federico García Lorca ambientado en la Zaragoza de la posguerra.', 120);

-- ---------------------------------------------------------------------
-- Asientos: 12 en la sala 1, 8 en la sala 2.
-- ---------------------------------------------------------------------
INSERT INTO asiento (id, sala_id, tipo_asiento_id, fila, numero) VALUES
  (1,  1, 2, 'A', 1), (2,  1, 2, 'A', 2), (3,  1, 2, 'A', 3), (4,  1, 2, 'A', 4),
  (5,  1, 1, 'B', 1), (6,  1, 1, 'B', 2), (7,  1, 1, 'B', 3), (8,  1, 1, 'B', 4),
  (9,  1, 1, 'C', 1), (10, 1, 1, 'C', 2), (11, 1, 1, 'C', 3), (12, 1, 1, 'C', 4),
  (13, 2, 1, 'A', 1), (14, 2, 1, 'A', 2), (15, 2, 1, 'A', 3), (16, 2, 1, 'A', 4),
  (17, 2, 2, 'B', 1), (18, 2, 2, 'B', 2), (19, 2, 2, 'B', 3), (20, 2, 2, 'B', 4);

-- ---------------------------------------------------------------------
-- Funciones
-- ---------------------------------------------------------------------
INSERT INTO funcion (id, obra_id, sala_id, fecha_hora, precio) VALUES
  (1, 1, 1, '2026-10-03 20:00:00', 5000.0),
  (2, 1, 1, '2026-10-04 20:00:00', 5000.0),
  (3, 2, 2, '2026-10-05 21:00:00', 7000.0);

-- ---------------------------------------------------------------------
-- ticket — comprobante de venta, UNO POR CADA BUTACA (1:1 con entrada).
--
-- Guarda un SNAPSHOT de los datos comerciales de la compra, no una FK a
-- compra: el comprobante debe seguir diciendo lo que se vendió en su momento,
-- aunque después se renombre la obra o se le cambie el precio. Esa es la
-- diferencia entre desnormalizar bien (un comprobante histórico es inmutable)
-- y desnormalizar mal (duplicar un dato que hay que mantener sincronizado).
--
-- Deliberadamente NO tiene usuario_id ni rol: la identidad del comprador es
-- dato de la compra, no del comprobante.
--
-- La FK a entrada sí se mantiene, pero con otro propósito: integridad
-- referencial para garantizar un solo ticket por butaca vendida.
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
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Una compra de ejemplo del cliente 4 con dos entradas ya COMPRADAS.
-- Sirve para probar el 409: al intentar comprar de nuevo el asiento 1 o 2
-- en la funcion 1, la base lo rechaza por el indice unico.
-- Los asientos LIBRES para probar en la funcion 1 son: 3, 4, 5, 6, 7, 8, 9, 10, 11, 12.
-- ---------------------------------------------------------------------
INSERT INTO compra (id, usuario_id, fecha, estado, total, monto_total) VALUES
  (1, 4, '2026-09-20 18:30:00', 'CONFIRMADA', 13500.00, 13500.0);

INSERT INTO entrada (compra_id, funcion_id, asiento_id, precio_pagado, estado, codigo) VALUES
  (1, 1, 1, 8500.00, 'ACTIVA', 'A1B2C3D4E5F6'),
  (1, 1, 2, 5000.00, 'ACTIVA', 'G7H8I9J0K1L2');

-- Los dos tickets correspondientes a esa compra de ejemplo: uno por butaca.
INSERT INTO ticket (codigo, fecha_emision, obra_titulo, obra_clasificacion,
                    funcion_fecha_hora, teatro_nombre, sala_nombre,
                    butaca_fila, butaca_numero, precio_pagado, entrada_id) VALUES
  ('TCK-EJEMPLO0001', '2026-09-20 18:30:00', 'El Enfermo Imaginario', 'ATP',
   '2026-10-03 20:00:00', 'Teatro Lanús', 'Sala Principal', 'A', 1, 8500.00, 1),
  ('TCK-EJEMPLO0002', '2026-09-20 18:30:00', 'El Enfermo Imaginario', 'ATP',
   '2026-10-03 20:00:00', 'Teatro Lanús', 'Sala Principal', 'A', 2, 5000.00, 2);
