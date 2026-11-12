-- Automatización 5 · Clasificador de emails del buzón general
CREATE TABLE email_entrante (
    id               BIGSERIAL PRIMARY KEY,
    remitente        VARCHAR(200)  NOT NULL,
    remitente_email  VARCHAR(150),
    asunto           VARCHAR(300),
    texto            VARCHAR(10000),
    recibido_en      TIMESTAMP     NOT NULL,
    categoria        VARCHAR(20)   NOT NULL,
    urgencia         VARCHAR(10)   NOT NULL,
    confianza        NUMERIC(4, 3),
    resumen          VARCHAR(500),
    datos            JSONB,
    cliente_id       BIGINT REFERENCES cliente (id),
    poliza_id        BIGINT REFERENCES poliza (id),
    identificado_por VARCHAR(20),
    avisos           VARCHAR(1000),
    asignado_a       VARCHAR(80)   NOT NULL,
    estado           VARCHAR(20)   NOT NULL,
    respuesta_asunto VARCHAR(300),
    respuesta        VARCHAR(5000),
    callback_url     VARCHAR(500),
    revisor          VARCHAR(80),
    error            VARCHAR(500),
    creado_en        TIMESTAMP     NOT NULL,
    borrador_en      TIMESTAMP,
    resuelto_en      TIMESTAMP,
    enviado_en       TIMESTAMP
);
CREATE INDEX ix_email_entrante_creado ON email_entrante (creado_en);

UPDATE automatizacion SET estado = 'EN_DESARROLLO' WHERE codigo = 'clasificador-email';
