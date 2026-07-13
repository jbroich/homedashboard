# Home Dashboard

Home Dashboard is a private smart-home backend for collecting room-climate
measurements and making them available through a REST API. It is intended to run
on a Raspberry Pi style deployment with PostgreSQL and MQTT.

## Current Scope

The `main` branch contains the backend, Docker deployment files, GitHub Actions
image publishing, and local sensor scripts. Some branches may additionally
contain an Expo/React Native frontend; merge that work intentionally instead of
mixing it into backend-only changes.

## Architecture

```text
sensors / Zigbee2MQTT -> MQTT -> Spring Boot -> PostgreSQL -> REST API
```

- Backend: Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA.
- Database: PostgreSQL for runtime, H2 for tests.
- MQTT client: Eclipse Paho.
- Deployment: Docker image built from `Dockerfile`, Compose stack in
  `backend/docker-compose.yml`.

## Repository Layout

- `backend/` - Spring Boot application, tests, Docker Compose files, env example.
- `backend/scripts/` - Python sensor scripts.
- `.github/workflows/build-and-push.yml` - GHCR image build for `main`.
- `AGENTS.md` - durable Codex context and working rules.

## Local Backend Development

Start only PostgreSQL locally:

```powershell
cd backend
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d postgres
```

Then run `HomedashboardApplication` from the IDE, or run tests directly:

```powershell
cd backend
.\mvnw.cmd test
```

Tests use H2 and do not require a live PostgreSQL database or MQTT broker.

## Configuration

Use `backend/.env.example` as the template for deployment configuration. Runtime
values belong in `backend/.env` or server environment variables, not in Git.

Important backend variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SERVER_PORT`
- `MQTT_ENABLED`
- `MQTT_BROKER_URL`
- `MQTT_TOPIC`
- `MQTT_CLIENT_ID`

Local application defaults keep MQTT disabled unless it is explicitly enabled.

## API

Current measurement endpoints:

```text
GET /api/measurements/{room}/latest
GET /api/measurements/{room}/chart/{range}
GET /api/measurements/{room}/chart/{range}?to=2026-04-30T23:59:59%2B02:00
```

Supported chart ranges are defined in `ChartRange`.

## Raspberry Pi Deployment

On the deployment host:

```bash
cd backend
cp .env.example .env
docker compose -f docker-compose.yml pull
docker compose -f docker-compose.yml up -d
```

Set real production values in `.env`. Do not commit that file.
