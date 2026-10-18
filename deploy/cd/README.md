# Prepared Home Dashboard deployment

These files capture the preparation made on the Pi on 2026-10-18. Nothing here
activates Komodo, installs cron, or deploys the application. Scripts currently
use `/home/jbroich/homedashboard` as the runtime directory. Copy `deploy/cd/`
to that directory as `cd/`; copy `deploy/komodo/compose.yaml` to
`/home/jbroich/komodo/compose.yaml`. Review paths before use on another host.

## Observed host

ARM64, four Cortex-A72 cores, 3.7 GiB RAM with approximately 2.7 GiB available.
The root filesystem had 16 GiB free. Docker memory statistics reported zero
and could not be used. Home Dashboard runs as Compose project `homedashboard`
with PostgreSQL 15 volume `homedashboard_pgdata`, backend port 8081, frontend
port 8082, and external network `zigbee-stack_default`. Zigbee2MQTT and
Mosquitto also run on this host. Cloudflared was active; local frontend HTTP
returned 200. `/api/rooms` returned 404 and is not a usable health endpoint.

## Intended release flow

1. Finish and merge [PR #38](https://github.com/jbroich/homedashboard/pull/38).
   At inspection it was open at `ee143614c518963921830f7b8e6ca73d589ba4ce`.
   It builds ARM64 images tagged with the full commit SHA and `latest`.
2. Add a final CI release job requiring both image jobs, restricted to successful
   non-PR runs on `main`. Expose the build steps' digest outputs and produce one
   manifest from that run with `commit`, `backend`, and `frontend` fields.
   Image values must be `ghcr.io/jbroich/homedashboard-backend@sha256:<64 hex>`
   and `ghcr.io/jbroich/homedashboard-frontend@sha256:<64 hex>`.
   Verify ARM64 support. Never resolve `latest` to construct a release pair.
3. Configure an authenticated Komodo trigger. Before invoking the script, verify
   repository, branch, commit, run ID, both successful builds, and manifest
   provenance server-side. The script validates format, not CI provenance.
   CI integration and this verification are NOT implemented yet.
4. Configure a serial Komodo job invoking:

   ```bash
   HOMEDASHBOARD_BACKEND_HEALTH_URL=http://127.0.0.1:8081/VALID_ENDPOINT python3 /home/jbroich/homedashboard/cd/deploy.py /path/to/trusted-release.json
   ```

   This requires host networking, Python 3, Bash, flock, and Docker Compose.
   Periphery's container loopback is different from the host loopback: configure
   host execution or a suitable runner before using this command.
5. The script locks concurrent deployments, validates image digests, pulls images,
   and runs the backup script. Any backup command failure aborts before Compose up.
   Only backend and frontend are updated; PostgreSQL is not restarted.
6. The script requires HTTP 200 from the configured backend URL and local frontend
   before writing `deployed-release.json`. A real backend endpoint checking database
   access remains to be implemented, along with a Cloudflare reachability check.
   Health failure reports failure but does not automatically roll back application
   images or restore the database. Test the return to the previous application
   version before enabling automatic deployment.

## PostgreSQL backups

`backup.sh` uses pg_dump inside the running PostgreSQL container, custom format,
private file permissions, atomic dump publication, pg_restore archive listing,
and SHA256 output. Credentials stay inside the container. Dumps contain application
data and must never be committed. One initial backup was created successfully.
An archive listing and checksum do not prove a successful restore.

Next, restore a dump into a separate PostgreSQL 15 test instance and verify
measurement data. Never use the production volume for this test.

`backup.cron` is an uninstalled template for daily backups at 03:17. Verify host
timezone Europe/Berlin, Docker access, and failure notifications before installation.
Backups currently remain local, with no automatic retention deletion. Choose an
external target, encryption, retention, and storage monitoring before activation.

## Komodo preparation

Compose follows the official [FerretDB setup](https://komo.do/docs/setup/ferretdb)
and [upstream Compose](https://github.com/moghtech/komodo/blob/main/compose/ferretdb.compose.yaml).
Current official MongoDB ARM64 releases require CPU capabilities beyond Cortex-A72;
test the alternative's exact image and CPU compatibility on this Pi as well.
Komodo has a separate database and binds port 9120 only on host loopback.
Memory limits are initial, untested values.

Local `compose.env` contains generated secrets and is intentionally excluded.
Recreate it securely on a new host using upstream configuration guidance.
Required settings include database credentials, initial admin credentials, JWT
and webhook secrets, registration disabled, and Core/Periphery communication keys.
On the prepared Pi, that private file already exists. Do not print or commit it.

Create private `images.env` defining DOCUMENTDB_IMAGE, FERRETDB_IMAGE, CORE_IMAGE,
and PERIPHERY_IMAGE with compatible, verified ARM64 digest references. Core and
Periphery versions must match. Missing image settings intentionally prevent startup.
Then validate from `/home/jbroich/komodo`:

```bash
docker compose -p komodo --env-file compose.env --env-file images.env -f compose.yaml config --quiet
```

Start Komodo separately only after image compatibility checks. Verify login and
server connection. Do not import/deploy the application automatically or let Komodo
overwrite the existing application `.env`. Configure an SSH tunnel or protected
Cloudflare access and back up Komodo's own database and keys.

## Validation and remaining work

Bash/Python syntax was checked. A mocked backup failure confirmed no Compose up
is reached. A real backup passed dump creation and archive listing. Restore testing,
image compatibility, Komodo execution, authenticated CI integration, database health
checks, rollback testing, daily scheduling, alerting, and offsite backups remain open.
