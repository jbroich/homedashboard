#!/usr/bin/env python3
"""Restore a backup into a disposable PostgreSQL 15 container, never production."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import uuid


def verify_backup(path):
    checksum = Path(str(path) + '.sha256').read_text().split()[0]
    if not re.fullmatch(r'[0-9a-f]{64}', checksum):
        raise ValueError('Invalid SHA256 sidecar')
    with path.open('rb') as stream:
        actual = hashlib.file_digest(stream, 'sha256').hexdigest()
    if actual != checksum:
        raise ValueError('Backup checksum does not match')


def restore_test(path, minimum_measurements=1):
    verify_backup(path)
    # No network, host ports, bind mounts, or production volume references.
    result = subprocess.run([
        'docker', 'run', '--detach', '--network', 'none',
        '--name', 'homedashboard-restore-' + uuid.uuid4().hex,
        '--label', 'com.homedashboard.purpose=restore-test',
        '--env', 'POSTGRES_HOST_AUTH_METHOD=trust', 'postgres:15',
    ], check=True, text=True, capture_output=True)
    container = result.stdout.strip()
    try:
        for attempt in range(60):
            ready = subprocess.run([
                'docker', 'exec', container, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres',
            ], capture_output=True)
            if ready.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError('Restore test database did not start')
        with path.open('rb') as archive:
            subprocess.run([
                'docker', 'exec', '-i', container, 'pg_restore',
                '--exit-on-error', '--single-transaction', '--no-owner', '--no-acl',
                '-U', 'postgres', '-d', 'postgres',
            ], stdin=archive, check=True)
        result = subprocess.run([
            'docker', 'exec', container, 'psql', '-U', 'postgres', '-d', 'postgres',
            '-v', 'ON_ERROR_STOP=1', '-At', '-c',
            "SELECT json_build_object('measurements', count(*), 'oldest', min(ts), 'newest', max(ts)) FROM public.measurement",
        ], check=True, text=True, capture_output=True)
        summary = json.loads(result.stdout)
        if summary['measurements'] < minimum_measurements:
            raise RuntimeError('Restored measurement count is below the expected minimum')
        return summary
    finally:
        # Remove only this test's container and its anonymous volume.
        subprocess.run(['docker', 'rm', '--force', '--volumes', container], check=True, stdout=subprocess.DEVNULL)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('backup', type=Path)
    parser.add_argument('--minimum-measurements', type=int, default=1)
    args = parser.parse_args()
    if args.minimum_measurements < 0:
        parser.error('--minimum-measurements must be nonnegative')
    print(json.dumps(restore_test(args.backup, args.minimum_measurements), indent=2))


if __name__ == '__main__':
    main()
