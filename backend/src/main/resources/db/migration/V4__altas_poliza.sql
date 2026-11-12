-- Automatización 3: alta de pólizas desde PDF (seguimiento de cada documento subido desde la app)
CREATE TABLE alta_poliza (
    id             BIGSERIAL PRIMARY KEY,
    fichero        VARCHAR(255)  NOT NULL,
    estado         VARCHAR(20)   NOT NULL,
    poliza_numero  VARCHAR(30),
    motivos        VARCHAR(2000),
    creada_por     VARCHAR(80),
    creada_en      TIMESTAMP     NOT NULL,
    resuelta_en    TIMESTAMP
);
CREATE INDEX ix_alta_poliza_creada_en ON alta_poliza (creada_en);
