# Codex Project Context

## Project

Home Dashboard is a private smart-home project for collecting and presenting
room-climate measurements. The backend ingests sensor data, stores measurements
in PostgreSQL, and exposes REST endpoints for dashboards and future clients.

The `main` branch currently contains the backend, frontend, and app-level Docker
stack. Keep documentation focused on the intended product shape instead of old
local artifacts.

## Architecture

- `docker-compose.yml` is the app-level Raspberry Pi runtime stack.
- `backend/` contains the Spring Boot API, JPA model, MQTT ingestion, tests, and
  backend image definition.
- `frontend/` contains the Expo client, static web export, and frontend image
  definition.
- `.github/workflows/build-and-push.yml` builds and pushes backend and frontend
  images to GHCR from `main`.

The intended data flow is:

```text
sensors / Zigbee2MQTT -> MQTT -> Spring Boot backend -> PostgreSQL -> REST API
```

The frontend API base URL is configured at build time through
`EXPO_PUBLIC_API_BASE_URL`. Local development uses `frontend/.env.local`.
The image workflow reads the GitHub Actions repository variable of the same
name, defaulting to `/` for the current frontend container's `/api/` proxy.
A future separate API gateway can be selected through that build variable.
Changing the deployed API base URL requires rebuilding the frontend image;
Compose runtime variables do not configure browser JavaScript.

## Agent Focus Areas

- Backend agent: Spring Boot, MQTT, PostgreSQL, API contracts, tests.
- Frontend agent: Expo, React Native, TypeScript, API client, dashboard UI.
- DevOps agent: Docker, Raspberry Pi deployment, GHCR, environment variables.
- QA/review agent: regression risks, missing tests, and verification commands.
- Product/backlog agent: split roadmap ideas into small implementable tasks.

Use these as working lenses, not as separate frameworks. Prefer the smallest
agent focus that matches the task.

## Repository Rules

- Keep real secrets, tokens, passwords, broker URLs, and production hostnames out
  of Git.
- Commit only example values in `.env.example` files.
- Do not rely on local-only notes in `.local-notes/`; promote durable project
  facts into tracked README or `AGENTS.md` files.
- Use English for documentation, code comments, branch names, and commit
  messages unless the user explicitly asks otherwise.
- Do not mix unrelated feature work into context/documentation branches.
- Preserve user changes in the worktree. Do not reset, checkout, or remove user
  work unless explicitly requested.

## Commands

From the repository root:

```powershell
cd backend
.\mvnw.cmd test
```

Start a local PostgreSQL container for backend debugging:

```powershell
docker compose -f docker-compose.local.yml up -d
```

Run the local development stack with PostgreSQL in Docker and the applications
natively:

```powershell
# Terminal 1: backend
cd backend
.\mvnw.cmd -Dspring-boot.run.profiles=dev spring-boot:run

# Terminal 2: frontend
cd frontend
Copy-Item .env.example .env.local
npx expo start --web --port 19006
```

The local PostgreSQL database is available only on `127.0.0.1:15432`. Reset its
sample data with `docker compose -f docker-compose.local.yml down -v`, then
start it again.

Refresh the Compose stack on the Raspberry Pi host:

```bash
cd /opt/homedashboard
docker compose pull
docker compose up -d
```

## Verification

- For backend code changes, run `.\mvnw.cmd test` from `backend/`.
- For Docker/deployment changes, validate Compose syntax and environment
  variable names against `.env.example`.
- For documentation-only changes, tests are not required unless the docs claim a
  command or behavior that should be verified.
