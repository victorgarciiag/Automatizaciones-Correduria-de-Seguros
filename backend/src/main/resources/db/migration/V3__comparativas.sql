-- Automatización 2 (v2): comparativas de presupuestos lanzadas desde la app
CREATE TABLE comparativa (
    id                BIGSERIAL PRIMARY KEY,
    cliente_nombre    VARCHAR(150)   NOT NULL,
    cliente_email     VARCHAR(150)   NOT NULL,
    vehiculo          VARCHAR(150)   NOT NULL,
    num_presupuestos  INT            NOT NULL,
    estado            VARCHAR(30)    NOT NULL,
    datos             JSONB,
    email_asunto      VARCHAR(200),
    email_html        VARCHAR(20000),
    callback_url      VARCHAR(500),
    error             VARCHAR(1000),
    creada_por        VARCHAR(80),
    revisor           VARCHAR(80),
    creada_en         TIMESTAMP      NOT NULL,
    borrador_en       TIMESTAMP,
    resuelta_en       TIMESTAMP,
    enviada_en        TIMESTAMP
);
CREATE INDEX ix_comparativa_creada_en ON comparativa (creada_en);
