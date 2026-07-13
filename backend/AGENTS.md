# Backend Context

## Scope

This file applies to `backend/`. The backend is a Java 21 Spring Boot service
that stores room measurements, exposes REST endpoints, and optionally subscribes
to MQTT topics for live sensor data.

## Stack

- Java 21
- Spring Boot 3.4.2
- Spring Web
- Spring Data JPA
- PostgreSQL at runtime
- H2 for tests
- Eclipse Paho MQTT client
- Maven wrapper

## Runtime Configuration

Application defaults live in `src/main/resources/application.properties`.
Deployment values are supplied through environment variables. The app-level
Compose stack reads `.env` from the repository root or deployment directory.

Rules:

- Do not add fixed LAN IPs, real hostnames, passwords, or tokens as committed
  defaults.
- Keep local defaults developer-friendly and production values environment-only.
- Keep `MQTT_ENABLED=false` as the safe local default unless the user explicitly
  asks otherwise.
- If adding or renaming an environment variable, update the root `.env.example`
  and any affected docs.

## MQTT

`MqttSubscriber` subscribes to `zigbee2mqtt/+` style topics and maps known
device names to `Room` values. MQTT should be treated as an optional ingestion
path: API and test behavior should not require a reachable broker.

If changing startup behavior, prefer graceful degradation:

- Backend starts even when MQTT is disabled.
- Tests do not open real MQTT connections.
- Broker outages should be logged and handled without breaking unrelated API
  behavior.

## REST API

Current controller scope:

```text
GET /api/measurements/{room}/latest
GET /api/measurements/{room}/chart/{range}
GET /api/measurements/{room}/chart/{range}?to=<offset-date-time>
```

Parsing should remain tolerant of supported room/range aliases through the model
helpers. Invalid room, range, or timestamp input should return `400`; missing
latest measurements should return `404`.

## Tests

Run backend verification from `backend/`:

```powershell
.\mvnw.cmd test
```

Add focused tests when changing:

- chart range logic
- controller status codes or response shapes
- measurement aggregation
- room/device mapping
- MQTT parsing behavior

## Docker

`backend/Dockerfile` builds the backend image from the `backend/` Docker
context. The root Compose stack uses the GHCR image and expects environment
variables from a root `.env`. The local Compose override exposes PostgreSQL on
`127.0.0.1:5432` for IDE debugging.
