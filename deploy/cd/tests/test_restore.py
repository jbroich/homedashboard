import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import unittest
import uuid
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('restore_test', Path(__file__).parents[1] / 'restore-test.py')
restore = importlib.util.module_from_spec(spec)
spec.loader.exec_module(restore)


class RestoreTest(unittest.TestCase):
    def test_checksum_failure_never_starts_a_container(self):
        with tempfile.TemporaryDirectory() as directory:
            backup = Path(directory) / 'postgres.dump'
            backup.write_bytes(b'corrupted archive')
            Path(str(backup) + '.sha256').write_text('a' * 64 + '  postgres.dump\n')
            with patch.object(restore.subprocess, 'run') as run:
                with self.assertRaisesRegex(ValueError, 'checksum does not match'):
                    restore.restore_test(backup)
            run.assert_not_called()

    @unittest.skipUnless(os.environ.get('HOMEDASHBOARD_RUN_DOCKER_TESTS') == '1', 'Set HOMEDASHBOARD_RUN_DOCKER_TESTS=1 for the PostgreSQL restore test')
    def test_restores_measurements_into_a_disposable_database(self):
        source = subprocess.run([
            'docker', 'run', '--detach', '--network', 'none',
            '--name', 'homedashboard-backup-fixture-' + uuid.uuid4().hex,
            '--env', 'POSTGRES_HOST_AUTH_METHOD=trust', 'postgres:15',
        ], check=True, capture_output=True, text=True).stdout.strip()
        try:
            for attempt in range(60):
                result = subprocess.run(['docker', 'exec', source, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres'], capture_output=True)
                if result.returncode == 0:
                    break
                time.sleep(1)
            else:
                self.fail('Fixture database did not start')
            subprocess.run([
                'docker', 'exec', source, 'psql', '-U', 'postgres', '-v', 'ON_ERROR_STOP=1', '-c',
                "CREATE TABLE measurement (id bigint, ts timestamptz, room text, temperature_c double precision); "
                "INSERT INTO measurement VALUES (1, '2026-01-01T00:00:00Z', 'BEDROOM', 20), (2, '2026-01-02T00:00:00Z', 'OFFICE', 21);",
            ], check=True, capture_output=True)
            with tempfile.TemporaryDirectory() as directory:
                backup = Path(directory) / 'postgres.dump'
                with backup.open('wb') as output:
                    subprocess.run([
                        'docker', 'exec', source, 'pg_dump', '-U', 'postgres', '-d', 'postgres',
                        '--format=custom', '--no-owner', '--no-acl',
                    ], check=True, stdout=output)
                checksum = hashlib.sha256(backup.read_bytes()).hexdigest()
                Path(str(backup) + '.sha256').write_text(checksum + '  postgres.dump\n')
                summary = restore.restore_test(backup, minimum_measurements=2)
                self.assertEqual(summary['measurements'], 2)
                self.assertEqual(summary['oldest'], '2026-01-01T00:00:00+00:00')
                self.assertEqual(summary['newest'], '2026-01-02T00:00:00+00:00')
        finally:
            subprocess.run(['docker', 'rm', '--force', '--volumes', source], check=True, stdout=subprocess.DEVNULL)


if __name__ == '__main__':
    unittest.main()
