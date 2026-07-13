# Home Dashboard

Home Dashboard is a private smart-home application for collecting room-climate
measurements and showing them in a dashboard. It is intended to run on a
Raspberry Pi style deployment with PostgreSQL, MQTT, a Spring Boot backend, and
an Expo frontend.

## Current Scope

The `main` branch contains the backend, frontend, Docker files, and GitHub
Actions image publishing.

## Architecture

```text
sensors / Zigbee2MQTT -> MQTT -> Spring Boot -> PostgreSQL -> REST API
```

- Backend: Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA.
- Frontend: Expo, React Native, TypeScript.
- Database: PostgreSQL for runtime, H2 for tests.
- MQTT client: Eclipse Paho.
- Docker: one backend image, one frontend image, and a root Compose stack.

## Repository Layout

- `docker-compose.yml` - Raspberry Pi runtime stack.
- `.env.example` - template for stack configuration.
- `backend/` - Spring Boot application, tests, backend image definition.
- `frontend/` - Expo application, static web export, frontend image definition.
- `.github/workflows/build-and-push.yml` - backend and frontend image builds for
  `main`.
- `AGENTS.md` - durable Codex context and working rules.

## Configuration

Use `.env.example` as the template for deployment configuration. Runtime values
belong in `.env` next to `docker-compose.yml` or in server environment
variables, not in Git.

Important runtime variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SERVER_PORT`
- `FRONTEND_PORT`
- `MQTT_ENABLED`
- `MQTT_BROKER_URL`
- `MQTT_TOPIC`
- `MQTT_CLIENT_ID`

Application defaults keep MQTT disabled unless it is explicitly enabled.

## API

Current measurement endpoints:

```text
GET /api/measurements/{room}/latest
GET /api/measurements/{room}/chart/{range}
GET /api/measurements/{room}/chart/{range}?to=2026-04-30T23:59:59%2B02:00
```

Supported chart ranges are defined in `ChartRange`.

## Raspberry Pi

The Raspberry Pi runs PostgreSQL, the backend image, and the frontend image via
`docker-compose.yml`.

To refresh the running stack:

```bash
cd /opt/homedashboard
docker compose pull
docker compose up -d
```

Set real production values in `.env`. Do not commit that file.
