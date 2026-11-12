-- Esquema inicial del backoffice de la correduría (datos 100 % ficticios)

CREATE TABLE aseguradora (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(10)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    email_siniestros VARCHAR(150),
    telefono         VARCHAR(20),
    color            VARCHAR(7)   NOT NULL DEFAULT '#64748b'
);

CREATE TABLE cliente (
    id         BIGSERIAL PRIMARY KEY,
    tipo       VARCHAR(20)  NOT NULL,
    nombre     VARCHAR(150) NOT NULL,
    nif        VARCHAR(12)  NOT NULL UNIQUE,
    telefono   VARCHAR(20),
    email      VARCHAR(150),
    direccion  VARCHAR(200),
    localidad  VARCHAR(80),
    fecha_alta DATE         NOT NULL
);
CREATE INDEX ix_cliente_telefono ON cliente (telefono);

CREATE TABLE poliza (
    id                BIGSERIAL PRIMARY KEY,
    numero            VARCHAR(30)    NOT NULL UNIQUE,
    cliente_id        BIGINT         NOT NULL REFERENCES cliente (id),
    aseguradora_id    BIGINT         NOT NULL REFERENCES aseguradora (id),
    ramo              VARCHAR(20)    NOT NULL,
    producto          VARCHAR(100)   NOT NULL,
    objeto_asegurado  VARCHAR(200)   NOT NULL,
    matricula         VARCHAR(10),
    fecha_efecto      DATE           NOT NULL,
    fecha_vencimiento DATE           NOT NULL,
    prima_anual       NUMERIC(10, 2) NOT NULL,
    periodicidad      VARCHAR(20)    NOT NULL,
    comision_pct      NUMERIC(5, 2)  NOT NULL,
    estado            VARCHAR(20)    NOT NULL
);
CREATE INDEX ix_poliza_cliente ON poliza (cliente_id);
CREATE INDEX ix_poliza_matricula ON poliza (matricula);
CREATE INDEX ix_poliza_vencimiento ON poliza (fecha_vencimiento);

CREATE TABLE recibo (
    id                    BIGSERIAL PRIMARY KEY,
    numero                VARCHAR(30)    NOT NULL UNIQUE,
    poliza_id             BIGINT         NOT NULL REFERENCES poliza (id),
    importe               NUMERIC(10, 2) NOT NULL,
    fecha_emision         DATE           NOT NULL,
    fecha_cobro           DATE,
    estado                VARCHAR(20)    NOT NULL,
    intentos_reclamacion  INT            NOT NULL DEFAULT 0
);
CREATE INDEX ix_recibo_poliza ON recibo (poliza_id);
CREATE INDEX ix_recibo_estado ON recibo (estado);

CREATE TABLE siniestro (
    id                    BIGSERIAL PRIMARY KEY,
    numero                VARCHAR(30)    NOT NULL UNIQUE,
    poliza_id             BIGINT         NOT NULL REFERENCES poliza (id),
    tipo                  VARCHAR(80)    NOT NULL,
    descripcion           VARCHAR(2000)  NOT NULL,
    fecha_ocurrencia      DATE           NOT NULL,
    fecha_apertura        TIMESTAMP      NOT NULL,
    estado                VARCHAR(30)    NOT NULL,
    canal                 VARCHAR(20)    NOT NULL,
    origen                VARCHAR(20)    NOT NULL,
    tramitador            VARCHAR(80),
    documentos_pendientes VARCHAR(500),
    importe_estimado      NUMERIC(10, 2)
);
CREATE INDEX ix_siniestro_estado ON siniestro (estado);

-- Catálogo de automatizaciones (planificadas y activas)
CREATE TABLE automatizacion (
    codigo              VARCHAR(40)  PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(500) NOT NULL,
    area                VARCHAR(40)  NOT NULL,
    estado              VARCHAR(20)  NOT NULL,
    minutos_manuales    INT          NOT NULL,
    orden               INT          NOT NULL
);

-- Bandeja de revisión humana: lo que la IA propone y una persona aprueba o rechaza
CREATE TABLE revision (
    id                BIGSERIAL PRIMARY KEY,
    automatizacion    VARCHAR(40)  NOT NULL REFERENCES automatizacion (codigo),
    titulo            VARCHAR(200) NOT NULL,
    motivo            VARCHAR(500),
    datos_propuestos  JSONB        NOT NULL,
    datos_finales     JSONB,
    confianza         NUMERIC(4, 3),
    estado            VARCHAR(20)  NOT NULL,
    callback_url      VARCHAR(500),
    callback_estado   VARCHAR(200),
    revisor           VARCHAR(80),
    comentario        VARCHAR(500),
    creada_en         TIMESTAMP    NOT NULL,
    resuelta_en       TIMESTAMP,
    demo              BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_revision_estado ON revision (estado);

-- Registro de cada ejecución de una automatización (trazabilidad + coste)
CREATE TABLE ejecucion (
    id                 BIGSERIAL PRIMARY KEY,
    automatizacion     VARCHAR(40)    NOT NULL REFERENCES automatizacion (codigo),
    inicio             TIMESTAMP      NOT NULL,
    duracion_ms        INT            NOT NULL,
    resultado          VARCHAR(20)    NOT NULL,
    tokens_entrada     INT            NOT NULL DEFAULT 0,
    tokens_salida      INT            NOT NULL DEFAULT 0,
    coste_eur          NUMERIC(10, 5) NOT NULL DEFAULT 0,
    minutos_ahorrados  NUMERIC(6, 1)  NOT NULL DEFAULT 0,
    referencia         VARCHAR(100),
    detalle            VARCHAR(1000),
    n8n_execution_id   VARCHAR(50),
    demo               BOOLEAN        NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_ejecucion_inicio ON ejecucion (inicio);
CREATE INDEX ix_ejecucion_automatizacion ON ejecucion (automatizacion);

INSERT INTO automatizacion (codigo, nombre, descripcion, area, estado, minutos_manuales, orden) VALUES
('comparativa-presupuestos', 'Comparativa de presupuestos', 'PDFs de varias aseguradoras → tabla comparativa y email para el cliente', 'Comercial', 'EN_DESARROLLO', 25, 1),
('alta-poliza', 'Alta de pólizas desde PDF', 'PDF de la aseguradora → datos de cliente y póliza validados en la cartera', 'Emisión', 'PLANIFICADA', 8, 2),
('apertura-siniestro', 'Siniestros por mensajería', 'Fotos, audios y texto del cliente → expediente abierto y documentación solicitada', 'Siniestros', 'PLANIFICADA', 20, 3),
('recibos-devueltos', 'Recibos devueltos', 'Recordatorios escalonados al cliente y escalado a administración', 'Administración', 'PLANIFICADA', 4, 4),
('clasificador-email', 'Clasificador de emails', 'Clasifica el buzón general, extrae datos y asigna a la persona correcta', 'Atención al cliente', 'PLANIFICADA', 2, 5),
('consulta-cobertura', 'Consultas de cobertura (RAG)', 'Responde "¿esto me lo cubre?" citando la cláusula del condicionado', 'Atención al cliente', 'PLANIFICADA', 10, 6),
('renovaciones', 'Renovaciones', 'Detecta vencimientos y subidas de prima, avisa al cliente y crea tareas', 'Emisión', 'PLANIFICADA', 5, 7),
('conciliacion-comisiones', 'Conciliación de comisiones', 'Cruza las liquidaciones de las aseguradoras con la cartera', 'Contabilidad', 'PLANIFICADA', 90, 8),
('resumen-gerencia', 'Resumen diario para gerencia', 'Resumen del día: siniestros, recibos, renovaciones en riesgo', 'Gerencia', 'PLANIFICADA', 15, 9);
