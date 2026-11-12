# Correduría de Seguros · Backoffice

Backoffice de una **correduría de seguros ficticia**, pensado como escenario para construir y enseñar automatizaciones con IA (n8n + Claude).

> Empresa, personas y datos 100 % inventados.

La app hace el papel del "programa de gestión" que usaría el equipo de la correduría. **Las automatizaciones no viven aquí**: están en n8n, que lee y escribe en la app a través de su API. Así una demo se ve como en una empresa real: el cliente manda algo → n8n + Claude lo procesan → el resultado aparece en el backoffice.

---

## Qué incluye

| Pantalla | Para qué sirve en las demos |
|---|---|
| **Inicio** | KPIs de la cartera, horas ahorradas, pendientes de revisión, vencimientos y siniestros |
| **Clientes / Pólizas / Recibos** | La cartera: ~300 clientes, ~500 pólizas, recibos con devoluciones |
| **Siniestros** | Tablero kanban (arrastrar para cambiar de estado). Los abiertos por IA llevan la etiqueta «IA» |
| **Presupuestos** | Sube PDFs de varias aseguradoras → comparativa + email que una persona aprueba (automatización 2) |
| **Altas de pólizas** | Sube PDFs de pólizas → alta automática o a revisión (automatización 3) |
| **Buzón** | Emails del buzón general clasificados, con cliente identificado y respuesta sugerida (automatización 5) |
| **Bandeja de revisión** | Lo que propone la IA. La persona puede **corregir los datos** y aprobar o rechazar |
| **Automatizaciones** | Catálogo, ejecuciones, % de éxito, coste de IA, horas y euros ahorrados |

## Arquitectura

```
Telegram / email / PDF ──► n8n ◄──► API de Claude
                            │
                            ▼  /api/integracion/**  (cabecera X-API-Key)
        ┌─────────────── backend (Spring Boot 3 · Java 21) ───────────────┐
        │  PostgreSQL 16 · Flyway · datos de ejemplo generados al arrancar │
        └──────────────────────────────▲───────────────────────────────────┘
                                       │  /api/**
                        frontend (Angular 18 · nginx)
```

```
correduria/
├── backend/        Spring Boot (dominio, repositorios, servicios, API REST, seeder)
├── frontend/       Angular standalone + signals, sin librerías de UI
├── docker-compose.yml   postgres + backend + frontend + n8n
└── .env.example
```

---

## Arrancar

### Opción A · Todo con Docker (recomendada)

```bash
cp .env.example .env          # cambia INTEGRACION_API_KEY
docker compose up -d --build
```

El servicio `n8n` es el mismo que ya tenías (mismo volumen `n8n_data`), así que tus flujos se conservan. Tu `docker-compose.yml` original está guardado como `docker-compose.n8n-original.yml`.

| Servicio | URL |
|---|---|
| Backoffice | http://localhost:4200 |
| API + Swagger | http://localhost:8080/swagger-ui.html |
| n8n | http://localhost:5678 |

La primera vez tarda unos minutos (descarga dependencias de Maven y npm). La base de datos se rellena sola al arrancar.

### Opción B · Desarrollo local (para tocar código)

```bash
# 1. Solo la base de datos
docker compose up -d postgres

# 2. Backend (puerto 8080)
cd backend
mvn spring-boot:run

# 3. Frontend (puerto 4200, con proxy de /api hacia el 8080)
cd frontend
npm install
npm start
```

---

## Cómo se conecta n8n

Todos los endpoints de integración están en `/api/integracion/**` y exigen la cabecera `X-API-Key`. El frontend no puede usarlos y n8n no puede usar nada más (**mínimo privilegio**).

Desde n8n (dentro de Docker) la URL base es `http://backend:8080`. Desde tu Mac, `http://localhost:8080`.

| Método | Endpoint | Para qué |
|---|---|---|
| GET | `/api/integracion/clientes?telefono=` / `?nif=` / `?email=` | Identificar al cliente que escribe |
| GET | `/api/integracion/polizas?matricula=` / `?numero=` / `?nif=` | Buscar la póliza (incluye `enVigor`) |
| POST | `/api/integracion/siniestros` | Abrir un siniestro (valida que la póliza esté en vigor) |
| POST | `/api/integracion/revisiones` | Mandar algo a la bandeja de revisión humana |
| GET | `/api/integracion/revisiones/{id}` | Consultar cómo se resolvió |
| GET / POST | `/api/integracion/recibos/avisos-pendientes`, `/recibos/{id}/aviso-enviado` | Recibos devueltos (automatización 1) |
| POST | `/api/integracion/comparativas/{id}/borrador`, `/enviada`, `/error` | Comparativa de presupuestos (2) |
| POST | `/api/integracion/polizas/alta`, `/altas-poliza/{id}/resultado` | Alta de pólizas: validar + dar de alta (3) |
| POST | `/api/integracion/emails`, `/emails/{id}/borrador`, `/enviado`, `/error` | Clasificador de emails (5) |
| POST | `/api/integracion/ejecuciones` | Registrar la ejecución (coste, tokens, ahorro) |

### Ejemplos

```bash
API=http://localhost:8080/api/integracion
KEY="X-API-Key: cambia-esta-clave"

# ¿De quién es esta matrícula?
curl -H "$KEY" "$API/polizas?matricula=4821KBL"

# Pedir revisión humana
curl -H "$KEY" -H "Content-Type: application/json" -X POST $API/revisiones -d '{
  "automatizacion": "apertura-siniestro",
  "titulo": "Siniestro por Telegram · rotura de lunas",
  "motivo": "No se ha podido identificar la póliza",
  "confianza": 0.58,
  "datosPropuestos": { "telefono": "612 345 678", "tipo": "Rotura de lunas", "fechaOcurrencia": "2026-11-04" }
}'

# Registrar una ejecución (si no mandas minutosAhorrados, se usan los del catálogo)
curl -H "$KEY" -H "Content-Type: application/json" -X POST $API/ejecuciones -d '{
  "automatizacion": "comparativa-presupuestos",
  "resultado": "OK",
  "duracionMs": 8400,
  "tokensEntrada": 9200,
  "tokensSalida": 850,
  "costeEur": 0.037,
  "referencia": "Comparativa auto · 3 aseguradoras"
}'
```

### Patrón «la IA propone, la persona decide» con n8n

1. n8n crea la revisión con `callbackUrl = {{ $execution.resumeUrl }}`.
2. El flujo se queda esperando en un nodo **Wait → On Webhook Call**.
3. Alguien aprueba o rechaza en la Bandeja de revisión (puede corregir datos).
4. El backend llama a la `callbackUrl` con `{ revisionId, decision, datos, revisor, comentario }`.
5. n8n se despierta y sigue: si `decision = APROBADA`, usa `datos` (ya corregidos); si no, termina.

> n8n genera el `resumeUrl` con `http://localhost:5678/...`, pero dentro de Docker el backend no llega a n8n por `localhost`. Por eso el backend tiene `N8N_URL_INTERNA=http://n8n:5678` y reescribe esa parte de la URL al avisar.

---

## Datos de demostración

Al arrancar con la base de datos vacía se generan:

- **Cartera ficticia** (clientes, pólizas, recibos, siniestros), siempre la misma (semilla fija). NIF en el rango 90.000.000+, emails `@*.example`.
- **Ejecuciones y revisiones de ejemplo** (`demo = true`) para que el panel no esté vacío. La app lo indica con «Datos de demostración».

Antes de medir automatizaciones reales (y antes de grabar vídeos con métricas), bórralas con el botón **«Borrar datos demo»** del panel de Automatizaciones, o arranca con `SEED_EJECUCIONES_DEMO=false`.

Para empezar de cero: `docker compose down -v` (borra los volúmenes).

---

## Decisiones técnicas

- **Flyway** crea el esquema; Hibernate solo lo valida (`ddl-auto: validate`).
- **DTOs como `record`** y `open-in-view: false`: nunca se serializan entidades JPA.
- **Revisiones en JSONB**: cada automatización manda los campos que necesite sin tocar el esquema.
- **Catálogo de automatizaciones en BD** (tabla `automatizacion`): estado, área y minutos manuales por caso, que sirven para calcular el ahorro.
- **Coste por hora de empleado** configurable: `app.coste-hora-empleado` (18 €/h por defecto).
- **Sin autenticación de usuarios** en el frontend: es una demo local. El usuario «conectado» se elige en el menú lateral.

## Automatizaciones

| # | Automatización | Estado | Workflows (`workflows/`) |
|---|---|---|---|
| 1 | Recibos devueltos (sin IA) | Hecha | 01 |
| 2 | Comparativa de presupuestos | Hecha | 02 |
| 3 | Alta de pólizas desde PDF | Hecha | 03 |
| 5 | Clasificador de emails | Hecha | 05 (dos workflows: clasificar y enviar) |
| 10 | Consultar la empresa con Claude (MCP) | Siguiente | — |

Los webhooks que llama el backend usan la URL de **producción** de n8n (`/webhook/...`): el workflow tiene que estar **publicado**, y tras editarlo hay que volver a publicar.
