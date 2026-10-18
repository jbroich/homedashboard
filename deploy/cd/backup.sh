#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
root=${HOMEDASHBOARD_ROOT:-/opt/homedashboard}
mkdir -p "$root/backups"
exec 8>"$root/backups/.backup.lock"
flock -w 300 8
stamp=$(date -u +%Y%m%dT%H%M%SZ)
tmp=$(mktemp "$root/backups/.dump.XXXXXX")
trap 'rm -f -- "$tmp"' EXIT
compose=(docker compose --project-name homedashboard --project-directory "$root" --env-file "$root/.env" -f "$root/docker-compose.yml")
# Credentials stay inside the container; never source or print .env.
"${compose[@]}" exec -T postgres sh -ec 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-acl' > "$tmp"
test -s "$tmp"
"${compose[@]}" exec -T postgres pg_restore --list < "$tmp" > /dev/null
dest="$root/backups/postgres-$stamp-$$.dump"
mv "$tmp" "$dest"
(cd "$root/backups" && sha256sum "${dest##*/}") > "$dest.sha256"
printf 'Backup validated: %s\n' "$dest"
