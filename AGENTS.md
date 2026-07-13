# Codex Project Context

## Project

Home Dashboard is a private smart-home project for collecting and presenting
room-climate measurements. The backend ingests sensor data, stores measurements
in PostgreSQL, and exposes REST endpoints for dashboards and future clients.

The `main` branch currently contains the backend and deployment files. Keep
documentation focused on the intended product shape instead of old local
artifacts.

## Architecture

- `backend/` contains the Spring Boot API, JPA model, MQTT ingestion, tests, and
  Docker Compose deployment files.
- `Dockerfile` builds the backend image from the repository root and publishes a
  Java 21 runtime image.
- `.github/workflows/build-and-push.yml` builds and pushes the backend image to
  GHCR from `main`.

The intended data flow is:

```text
sensors / Zigbee2MQTT -> MQTT -> Spring Boot backend -> PostgreSQL -> REST API
```

When a `frontend/` directory is present, treat it as an Expo/React Native client
that talks to the backend REST API. Keep frontend-specific context in
`frontend/AGENTS.md` on branches where that directory is tracked.

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
cd backend
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d postgres
```

Deploy the backend Compose stack on the Raspberry Pi host:

```bash
cd backend
docker compose -f docker-compose.yml pull
docker compose -f docker-compose.yml up -d
```

## Verification

- For backend code changes, run `.\mvnw.cmd test` from `backend/`.
- For Docker/deployment changes, validate Compose syntax and environment
  variable names against `backend/.env.example`.
- For documentation-only changes, tests are not required unless the docs claim a
  command or behavior that should be verified.
