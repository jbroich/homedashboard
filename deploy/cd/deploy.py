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
import urllib.request

ROOT = Path('/home/jbroich/homedashboard')

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
    health = os.environ['HOMEDASHBOARD_BACKEND_HEALTH_URL']
    if not health.startswith('http://127.0.0.1:8081/'):
        raise SystemExit('Use a local backend health endpoint on port 8081')
    os.umask(0o077)
    with (ROOT / 'cd/.deploy.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        override = ROOT / 'cd/candidate.json'
        override.write_text(json.dumps({'services': {n: {'image': release[n]} for n in ('backend', 'frontend')}}))
        compose = ['docker', 'compose', '-p', 'homedashboard', '--project-directory', str(ROOT), '--env-file', str(ROOT / '.env'), '-f', str(ROOT / 'docker-compose.yml'), '-f', str(override)]
        run(*compose, 'config', '--quiet')
        run(*compose, 'pull', 'backend', 'frontend')
        # check=True is the hard backup gate. No compose up after any backup failure.
        run('bash', str(ROOT / 'cd/backup.sh'))
        run(*compose, 'up', '-d', '--no-deps', '--wait', '--wait-timeout', '180', 'backend', 'frontend')
        for attempt in range(60):
            try:
                for url in (health, 'http://127.0.0.1:8082/'):
                    with urllib.request.urlopen(url, timeout=5) as response:
                        if response.status != 200:
                            raise RuntimeError('Unhealthy HTTP response')
                (ROOT / 'cd/deployed-release.json').write_text(json.dumps(release, indent=2) + '\n')
                print('Deployment passed local HTTP checks')
                return
            except Exception:
                time.sleep(3)
        raise SystemExit('Health check failed. Manual application rollback required; database was not restored.')

if __name__ == '__main__':
    main()
