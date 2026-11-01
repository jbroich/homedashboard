# Home Dashboard

Home Dashboard is a private smart-home application for collecting room-climate
measurements and showing them in a dashboard. It uses PostgreSQL, MQTT, a
Spring Boot backend, and an Expo frontend. The current deployment uses Docker
Compose on a Raspberry Pi. The planned learning target is a Kubernetes
deployment on a VPS with an API gateway, CI, GitOps, and Argo CD.

## Current Scope

The repository contains the backend, frontend, local development setup, Docker
files, and GitHub Actions image publishing. Automated deployment, Kubernetes
manifests, an API gateway, and Argo CD configuration are not implemented yet.

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
- `docker-compose.local.yml` - local PostgreSQL for native application development.
- `.env.example` - template for stack configuration.
- `backend/` - Spring Boot application, tests, backend image definition.
- `frontend/` - Expo application, static web export, frontend image definition.
- `.github/workflows/build-and-push.yml` - backend and frontend image builds for
  `main`.
- `AGENTS.md` - durable Codex context and working rules.

## Configuration

Configuration is supplied at different stages:

| Context | Configuration source | When it takes effect |
| --- | --- | --- |
| Local backend | `backend/src/main/resources/application.properties` plus `application-dev.properties` with profile `dev` | Backend startup |
| Backend tests | `backend/src/test/resources/application.properties` (H2, MQTT disabled) | Test startup; no `test` profile required |
| Local frontend | `frontend/.env.local`, copied from `frontend/.env.example` | Expo bundles the app; reload the browser after changes |
| Compose deployment | Root `.env` or shell variables, passed through `docker-compose.yml` | Container creation/recreation |
| Published frontend | GitHub Actions repository variable `EXPO_PUBLIC_API_BASE_URL` | Image build |

Spring Boot does not automatically load `.env` files. The local `dev` profile
provides database settings directly; Spring also accepts environment variables
from the shell or IDE. With Compose, Compose reads the root `.env` and passes
the configured values into the containers. Expo loads its own frontend env
files. The root `.env` and `frontend/.env.local` serve different purposes.

Use the root `.env.example` as the template for Compose deployment configuration.
Runtime values belong in `.env` next to `docker-compose.yml` or in server
environment variables. Keep real credentials and deployment addresses out of Git.

Important runtime variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SERVER_PORT`
- `FRONTEND_PORT`
- `CORS_ALLOWED_ORIGINS`
- `MQTT_ENABLED`
- `MQTT_BROKER_URL`
- `MQTT_TOPIC`
- `MQTT_CLIENT_ID`

Application defaults and the local `dev` profile keep MQTT disabled. The root
deployment example enables it: configure a reachable broker or set
`MQTT_ENABLED=false` before deploying. Inside the backend container,
`localhost` refers to that container, not the Docker host or another machine.
An enabled but unreachable broker currently causes backend startup to fail.

## Local Development

Prerequisites: Java 21 (`JAVA_HOME` pointing to the JDK), Node.js 22 with npm,
and a running Docker engine with Docker Compose. Maven is provided by the
backend wrapper. Initial dependency installation requires internet access.
The commands below use PowerShell. On Linux/macOS, use `./mvnw` instead of
`.\mvnw.cmd` and `cp` instead of `Copy-Item`.

For normal local development, run PostgreSQL in Docker and both applications
natively. No root `.env`, MQTT broker, gateway, or Kubernetes cluster is needed.
Start each terminal in the repository root.

### 1. Start PostgreSQL

```powershell
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml exec postgres pg_isready -U homedashboard_local -d homedashboard_local
```

Wait until PostgreSQL reports that it is accepting connections. It is exposed
only on `127.0.0.1:15432`; its fixed local-only database name and credentials
match `application-dev.properties`. Data persists in a named Docker volume.

### 2. Start the backend

```powershell
cd backend
.\mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

The startup log should report the active profile `dev`. Spring loads the base
properties and applies the dev overrides: local PostgreSQL, SQL logging,
sample data initialization, and disabled MQTT. Environment variables set in
your shell or IDE override file values; remove unintended deployment overrides
such as `SPRING_DATASOURCE_URL` when using this local setup.

Flyway creates/migrates the schema, and Hibernate validates it. If the
`measurement` table is empty, the dev seed adds about 30 days of hourly sample
measurements for all rooms. Restarting the backend preserves existing data and
does not refresh the sample timestamps. Do not enable `dev` in deployment.

For IntelliJ, run `HomedashboardApplication` with the environment variable
`SPRING_PROFILES_ACTIVE=dev` in the Run/Debug configuration. This replaces the
Maven startup command above.

### 3. Start the frontend

```powershell
cd frontend
npm ci
if (-not (Test-Path .env.local)) { Copy-Item .env.example .env.local }
npx expo start --web --port 19006
```

Run `npm ci` on initial setup and after dependency lockfile changes. The copy
command preserves an existing `.env.local`; `.env.example` itself is a template
and is not loaded by Expo. For the current local setup, `.env.local` contains:

```dotenv
EXPO_PUBLIC_API_BASE_URL=http://localhost:8081
```

Open `http://localhost:19006` in the browser. The client appends
`/api/measurements/...` to the configured base URL, so do not add `/api` to it.
The backend's default CORS configuration allows this frontend origin.
Keep `.env.local` out of Git and fully reload the browser after changing it.
Once a local gateway exists, its address can replace the backend base URL.

These URLs assume the browser runs on the development computer. For a physical
phone or another computer, use a reachable address for the backend and adjust
`CORS_ALLOWED_ORIGINS` for a different browser origin as needed.

### 4. Verify and stop the stack

From an additional terminal, check the API:

```powershell
Invoke-RestMethod http://localhost:8081/api/measurements/livingroom/latest
```

The dashboard should display sample measurements. If startup fails, check that
Docker is running, PostgreSQL is ready, the backend has profile `dev`, and ports
15432, 8081, and 19006 are available. An old sample database may have no data in
the current chart window; use the reset below to generate fresh samples.

Stop the applications with `Ctrl+C` in their terminals. Stop PostgreSQL from
the repository root with:

```powershell
docker compose -f docker-compose.local.yml down
```

To **delete all local database data** and generate fresh samples, stop the
backend, then run from the repository root:

```powershell
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

Wait for PostgreSQL to be ready, then restart the backend with profile `dev`.

### Local checks

Backend tests use H2 and do not require Docker, PostgreSQL, or an MQTT broker.
From the repository root:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend checks, from a separate terminal at the repository root:

```powershell
cd frontend
npm run typecheck
npm run lint
npm run export:web
```

## Frontend Deployment Configuration

Expo embeds `EXPO_PUBLIC_API_BASE_URL` into the JavaScript during the web build.
The frontend image contains the exported files and Nginx to serve them.

The `build-and-push` workflow passes the GitHub Actions repository variable
`EXPO_PUBLIC_API_BASE_URL` as a Docker build argument. Configure it under
Settings > Secrets and variables > Actions > Variables, using the public API
base URL reachable from the user's browser, for example
`https://api.example.com`. The client appends `/api/measurements/...`, so do not
include `/api` in the base URL. The URL is public configuration, not a secret.

When the variable is unset, the workflow uses `/`: browser requests go through
the current frontend Nginx `/api/` proxy to the backend. A Spring Cloud gateway
is not part of the current stack. Once deployed separately, its public address
can be selected through the build variable. For a separate origin, configure
CORS to allow the frontend origin at the API entry point.

Using `/` can also support a future gateway on the frontend's public origin:
route `/api/` to the gateway and other paths to the frontend. That avoids an
environment-specific API hostname in the frontend bundle. Routing through a
separate gateway still needs to be implemented.

For a manual image build from the repository root:

```powershell
docker build --build-arg EXPO_PUBLIC_API_BASE_URL=https://api.example.com -t homedashboard-frontend ./frontend
```

Changing the API base URL requires a new frontend build and deployment. After
changing the repository variable, run the `build-and-push` workflow and refresh
the Pi stack. Setting a value in the Pi's `.env` or the frontend container's
runtime environment does not change the already built JavaScript. Backend
configuration such as database credentials is still supplied at container start.

## CI and Planned GitOps Deployment

The current `build-and-push` workflow runs on pushes to `main` and on manual
dispatch. It builds the backend with `./mvnw package` inside Docker, which runs
the backend tests. A separate frontend job runs typechecking and linting before
the frontend image build exports the web app. Both images are pushed to GHCR
with `latest` and commit-SHA tags. The backend and frontend publishing jobs can
succeed independently; this is not yet an atomic application release.

The intended deployment flow is:

```text
application commit -> CI checks and image builds -> GHCR
                                                   |
                         update image references in deployment Git
                                                   |
                                                Argo CD
                                                   |
                                           Kubernetes on VPS

browser -> API gateway / routing -> frontend and backend
sensors / Zigbee2MQTT -> MQTT -> backend -> PostgreSQL
```

Deployment Git will describe the desired Kubernetes state, including image
references, routing, and non-secret configuration. Argo CD will reconcile the
cluster with that state. Backend configuration will be supplied at pod startup
through environment variables sourced from ConfigMaps and Secrets. Secret
values must come from a secret-management mechanism or encrypted storage;
plain-text credentials must not be committed. Database credentials are not
needed to build either application image.

The following work remains before the VPS deployment:

- Build images for the VPS CPU architecture. The current workflow publishes
  only `linux/arm64`; an x86 VPS needs `linux/amd64` images.
- Add Kubernetes manifests or Helm/Kustomize configuration, persistent database
  storage, readiness checks, and the Argo CD application/bootstrap setup.
- Define how successful CI builds update deployment Git. Pin image references
  to a specific commit tag or digest so Git changes select the deployed release.
- Add the gateway and public TLS/routing configuration. The current frontend
  Nginx proxy uses Docker's `127.0.0.11` resolver and the Compose service name
  `backend`; adapt or bypass that proxy for Kubernetes routing.
- Provision secrets and registry access, and configure MQTT connectivity from
  the VPS to the sensor broker. Local development remains usable with MQTT off.

These are planned infrastructure changes. The current local workflow and the
separation between build-time frontend configuration and runtime backend
configuration are compatible with that direction.

References: [Spring Boot profiles](https://docs.spring.io/spring-boot/3.4/reference/features/profiles.html),
[Expo environment variables](https://docs.expo.dev/guides/environment-variables/),
and [Argo CD](https://argo-cd.readthedocs.io/en/stable/).

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
