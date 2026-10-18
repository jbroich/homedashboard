#!/usr/bin/env python3
"""Called by Komodo only after CI release provenance has been verified."""
import fcntl
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

ROOT = Path(os.environ.get('HOMEDASHBOARD_ROOT', '/opt/homedashboard'))


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def check_health(backend_url, frontend_url):
    # A redirected login page or an HTML response must never pass as backend health.
    opener = urllib.request.build_opener(NoRedirect)
    with opener.open(backend_url, timeout=5) as response:
        if response.status != 200 or json.load(response).get('status') != 'UP':
            raise RuntimeError('Backend or database is unhealthy')
    with opener.open(frontend_url, timeout=5) as response:
        if response.status != 200:
            raise RuntimeError('Frontend is unhealthy')


def write_json(path, value):
    temporary = path.with_name(path.name + '.tmp')
    temporary.write_text(json.dumps(value, indent=2) + '\n')
    temporary.replace(path)

def run(*args):
    subprocess.run(args, check=True)

def main():
    if len(sys.argv) != 2:
        raise SystemExit('Usage: deploy.py trusted-release.json')
    release = json.loads(Path(sys.argv[1]).read_text())
    if not re.fullmatch(r'[0-9a-f]{40}', release['commit']):
        raise SystemExit('Full commit SHA required')
    for name in ('backend', 'frontend'):
        if not re.fullmatch(r'ghcr\.io/jbroich/homedashboard-' + name + r'@sha256:[0-9a-f]{64}', release[name]):
            raise SystemExit('Expected repository and immutable digest required')
    # A known application endpoint must be configured before any mutation.
    health = os.environ.get('HOMEDASHBOARD_BACKEND_HEALTH_URL', 'http://127.0.0.1:8081/actuator/health')
    frontend = os.environ.get('HOMEDASHBOARD_FRONTEND_HEALTH_URL', 'http://127.0.0.1:8080/')
    for url in (health, frontend):
        parsed = urllib.parse.urlsplit(url)
        if parsed.scheme != 'http' or parsed.hostname != '127.0.0.1' or parsed.username or parsed.password:
            raise SystemExit('Use HTTP health endpoints on 127.0.0.1')
    if urllib.parse.urlsplit(health).path != '/actuator/health':
        raise SystemExit('Use the backend /actuator/health endpoint')
    os.umask(0o077)
    with (ROOT / 'cd/.deploy.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        state = ROOT / 'cd/deployed-release.json'
        if state.exists() and json.loads(state.read_text()) == release:
            print('Release is already deployed')
            return
        override = ROOT / 'cd/candidate.json'
        write_json(override, {'services': {n: {'image': release[n]} for n in ('backend', 'frontend')}})
        compose = ['docker', 'compose', '-p', 'homedashboard', '--project-directory', str(ROOT), '--env-file', str(ROOT / '.env'), '-f', str(ROOT / 'docker-compose.yml'), '-f', str(override)]
        run(*compose, 'config', '--quiet')
        run(*compose, 'pull', 'backend', 'frontend')
        # check=True is the hard backup gate. No compose up after any backup failure.
        run('bash', str(ROOT / 'cd/backup.sh'))
        run(*compose, 'up', '-d', '--no-deps', '--wait', '--wait-timeout', '180', 'backend', 'frontend')
        for attempt in range(60):
            try:
                check_health(health, frontend)
                break
            except Exception:
                time.sleep(3)
        else:
            raise SystemExit('Health check failed. Manual application rollback required; database was not restored.')
        if state.exists():
            previous = json.loads(state.read_text())
            if previous != release:
                write_json(ROOT / 'cd/previous-release.json', previous)
        write_json(ROOT / 'cd/active-images.json', {'services': {n: {'image': release[n]} for n in ('backend', 'frontend')}})
        write_json(state, release)
        print('Deployment passed local HTTP checks')

if __name__ == '__main__':
    main()
