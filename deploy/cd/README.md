# Home Dashboard deployment preparation

These files do not activate Komodo, install a schedule, or deploy the application.
The application remains a Docker Compose project; GitHub Actions builds its
images and Komodo will orchestrate deployment on the Pi.

## Runtime directory and settings

Copy this directory to `cd/` inside the existing application runtime directory.
That directory must contain the live `docker-compose.yml` and private `.env`.
Set `HOMEDASHBOARD_ROOT` to its absolute path; the default is `/opt/homedashboard`.
The prepared Pi currently uses `/home/jbroich/homedashboard`, project name
`homedashboard`, PostgreSQL 15, backend port 8081, and frontend port 8082.
Preserve the live Compose networks and existing database volume.

Export the settings shown in [.env.example](.env.example) in the native Periphery
execution environment. These are script settings, separate from the application's
private `.env`; the scripts never source or print that file. For the prepared Pi:

```bash
export HOMEDASHBOARD_ROOT=/home/jbroich/homedashboard
export HOMEDASHBOARD_BACKEND_HEALTH_URL=http://127.0.0.1:8081/actuator/health
export HOMEDASHBOARD_FRONTEND_HEALTH_URL=http://127.0.0.1:8082/
```

## Release contract and deployment

Finish and merge [PR #38](https://github.com/jbroich/homedashboard/pull/38) before
enabling release publishing. It gates the image builds on backend and frontend
verification and builds ARM64 images tagged with the full commit SHA.

The final `release-manifest` CI job requires both image jobs and publishes their
digest outputs together as the `homedashboard-release` artifact. It runs only
after pushes to `main`. The manifest has this shape (placeholder values shown):

```json
{
  "commit": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "repository": "jbroich/homedashboard",
  "run_id": 123,
  "backend": "ghcr.io/jbroich/homedashboard-backend@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
  "frontend": "ghcr.io/jbroich/homedashboard-frontend@sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
}
```

Never resolve `latest` independently to construct a release pair. `poll-release.py`
uses the GitHub CLI (`gh`) to verify the latest successful `main` push in the
expected repository/workflow. It requires successful backend/frontend checks,
both image jobs, release publication, and a successful `deployment-checks` workflow
for the same commit; verifies artifact commit and manifest
run ID; and validates the two immutable references. PR builds, missing checks,
expired artifacts, and mismatched manifests are rejected.

Install `gh` on the host and provide a dedicated credential with read access to
Actions artifacts via the native agent's private `GH_TOKEN` environment. Do not
commit this token or pass it as a command argument. First verify without deploying:

```bash
python3 "$HOMEDASHBOARD_ROOT/cd/poll-release.py"
```

Before the workflow from this branch and PR #38 are merged and successfully run
on `main`, this command intentionally fails instead of falling back to unverified
images. Artifacts are retained for 90 days; an expired release requires a new
successful build before a fresh installation can pick it up.

Once host validation is complete, schedule a serial Komodo procedure on the native
host agent (for example every five minutes) to invoke:

```bash
python3 "$HOMEDASHBOARD_ROOT/cd/poll-release.py" --deploy
```

This needs only outbound access to GitHub/GHCR, not a public deployment webhook.
An already deployed manifest is skipped inside the deployment lock. `deploy.py`
can also accept a trusted manifest directly for an operator-initiated deployment;
it checks format and repositories, not CI provenance. Keep that entry point local.

The deployment script locks deployments, validates immutable references, pulls both images,
and runs `backup.sh`. Any backup command failure aborts before Compose up. It
updates only backend and frontend, using `--no-deps`; PostgreSQL is not restarted.

After startup, the backend must return HTTP 200 with JSON `{"status":"UP"}` from
`/actuator/health`; this endpoint includes database connectivity. The frontend
must return HTTP 200. Redirects are rejected, so a login page cannot pass as health.
The backend image must include the Actuator changes from this branch.
`/api/rooms` is not an application endpoint and is unsuitable for this check.

After successful health checks, the script atomically writes each state file:

- `deployed-release.json`: the successfully deployed manifest.
- `previous-release.json`: the previous successful manifest, when one exists.
- `active-images.json`: a Compose override pinning the successful images.

For subsequent manual Compose operations, include `-f cd/active-images.json`
after `-f docker-compose.yml` to preserve the pins. Plain `docker compose up`
still uses whatever image references are in the base file.

A health failure reports failure without changing successful release state.
Candidate containers may already be running. Before the first rollout, record
the current image digests and document a manual application rollback: there is
no previous manifest until a successful deployment has been recorded. For later
releases, the retained manifest can be supplied to the same deployment script.
Test this path before enabling unattended deployment. The script never restores
the database automatically; schema compatibility must be assessed separately,
especially while Hibernate `ddl-auto=update` is in use.

## Backups and restore verification

`backup.sh` uses `pg_dump` inside the existing PostgreSQL container, custom format,
private file permissions, a backup lock, atomic dump publication, an archive
listing, and a SHA256 sidecar. Credentials stay inside the container. The dump
and sidecar must stay together when copied to external storage.

```bash
bash "$HOMEDASHBOARD_ROOT/cd/backup.sh"
python3 "$HOMEDASHBOARD_ROOT/cd/restore-test.py" /path/to/postgres-backup.dump
```

The restore test checks the checksum, creates an isolated PostgreSQL 15 container
without network access, published ports, or production mounts, and runs
`pg_restore --exit-on-error --single-transaction`. It queries the restored
`public.measurement` table and reports the row count and oldest/newest timestamps.
At least one row is required by default; use `--minimum-measurements N` when you
know a stronger lower bound. Only the test container and its anonymous volume
are removed afterward. Docker, Python 3.11+, and enough free disk space for a
second copy of the database are required. A checksum/archive listing alone does
not establish that restoration works.

An initial production backup was created on the Pi during preparation. That
specific dump still needs to be tested on the Pi. The automated integration test
restores a generated fixture with two measurements; it does not validate the
production dump.

`backup.cron` is an uninstalled daily 03:17 template. Adjust its root directory,
script path, and log path together for the actual installation. Create the log
directory first. Verify host timezone Europe/Berlin, Docker access, and failure
notifications before installing. Choose an external destination, encryption,
retention, and storage monitoring; no offsite copy or automatic deletion is
configured by these files.

## Komodo host execution

The prepared Pi has four Cortex-A72 cores, 3.7 GiB RAM, approximately 2.7 GiB
available memory, and 16 GiB free root disk at inspection on 2026-10-18.
Cloudflared, Zigbee2MQTT, and Mosquitto also run on this host.

The separate Komodo Compose follows the official [FerretDB setup](https://komo.do/docs/setup/ferretdb).
Current official MongoDB ARM64 images require CPU features beyond Cortex-A72.
The alternative's exact images still need an actual compatibility test on this
Pi. Memory limits are initial values and need observation under load.

Copy `deploy/komodo/compose.yaml` to a separate Komodo directory. Configure its
private `compose.env` using upstream guidance for the selected Core version.
The committed `.env.example` contains only database placeholders for syntax
validation, not a complete installation configuration. The Pi's existing private
file also contains initial admin credentials, JWT/webhook secrets, registration
settings, and communication keys; do not overwrite or print it.

Create private `images.env` defining `DOCUMENTDB_IMAGE`, `FERRETDB_IMAGE`, and
`CORE_IMAGE` with compatible, verified ARM64 digest references. Missing pins
prevent startup. From the Komodo directory, validate before starting:

```bash
docker compose -p komodo --env-file compose.env --env-file images.env -f compose.yaml config --quiet
```

Run **Periphery as a native systemd service**, matching the Core version, using
the official [server onboarding instructions](https://komo.do/docs/setup/connect-servers).
It must run as a user with access to Docker, the runtime directory, Python 3.11+,
Bash, `flock`, and Docker Compose. The containerized agent is intentionally not
part of this Compose: mounting the Docker socket alone does not provide host
tools or host loopback to commands running inside an agent container.

Core is bound only to host loopback on port 9120. Connect the native agent to
that address, verify login/server connectivity, and configure an SSH tunnel or
protected Cloudflare access for administration. Back up Komodo's database and
keys separately. Do not let a separate Komodo Stack bypass `deploy.py` by
independently enabling image auto-updates for the application.

## Verification and activation checklist

Run deployment tests on Linux:

```bash
HOMEDASHBOARD_RUN_DOCKER_TESTS=1 python3 -m unittest discover -s deploy/cd/tests -v
bash -n deploy/cd/backup.sh
```

CI runs these tests, including an isolated PostgreSQL restore, Compose validation,
and the backend suite. Backup-gate failure, unhealthy deployment state, mutable
image rejection, and successful release state retention have automated coverage.

Remaining host work: test the actual production dump, verify the pinned Komodo
images, onboard native Periphery, establish a baseline rollback, test a deployment,
configure the read-only GitHub credential, external backup destination and
monitoring, and activate scheduling. The first real manifest/Actions download
must be verified after merging the prerequisite PRs; it cannot be exercised
against a branch that has never published a production release artifact.
