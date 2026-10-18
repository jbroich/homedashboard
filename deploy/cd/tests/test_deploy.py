import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('deploy', Path(__file__).parents[1] / 'deploy.py')
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


class DeploymentTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        (self.root / 'cd').mkdir()
        self.release = {
            'commit': 'a' * 40,
            'backend': 'ghcr.io/jbroich/homedashboard-backend@sha256:' + 'b' * 64,
            'frontend': 'ghcr.io/jbroich/homedashboard-frontend@sha256:' + 'c' * 64,
        }
        self.manifest = self.root / 'release.json'
        self.manifest.write_text(json.dumps(self.release))
        self.state = self.root / 'cd/deployed-release.json'
        self.previous = dict(self.release, commit='d' * 40)
        self.state.write_text(json.dumps(self.previous))
        for target, value in [('ROOT', self.root)]:
            self.enterContext(patch.object(deploy, target, value))
        self.enterContext(patch.object(sys, 'argv', ['deploy.py', str(self.manifest)]))
        self.enterContext(patch.dict(os.environ, {}, clear=True))
        # main() deliberately changes the process umask; restore it after each test.
        old_umask = os.umask(0o077)
        self.addCleanup(os.umask, old_umask)

    def test_backup_failure_never_reaches_compose_up_or_changes_success_state(self):
        calls = []

        def run(*args):
            calls.append(args)
            if args[0] == 'bash':
                raise subprocess.CalledProcessError(1, args)

        with patch.object(deploy, 'run', side_effect=run), patch.object(deploy, 'check_health') as health:
            with self.assertRaises(subprocess.CalledProcessError):
                deploy.main()
        self.assertFalse(any('up' in args for args in calls))
        health.assert_not_called()
        self.assertEqual(json.loads(self.state.read_text()), self.previous)

    def test_success_backs_up_before_up_and_retains_previous_release(self):
        with patch.object(deploy, 'run') as run, patch.object(deploy, 'check_health'):
            deploy.main()
        calls = [call.args for call in run.call_args_list]
        backup = next(i for i, args in enumerate(calls) if args[0] == 'bash')
        update = next(i for i, args in enumerate(calls) if 'up' in args)
        self.assertLess(backup, update)
        self.assertIn('--no-deps', calls[update])
        self.assertEqual(calls[update][-2:], ('backend', 'frontend'))
        self.assertEqual(json.loads(self.state.read_text()), self.release)
        self.assertEqual(json.loads((self.root / 'cd/previous-release.json').read_text()), self.previous)
        active = json.loads((self.root / 'cd/active-images.json').read_text())
        self.assertEqual(active['services']['backend']['image'], self.release['backend'])

    def test_health_failure_does_not_record_candidate_as_successful(self):
        with patch.object(deploy, 'run'), patch.object(deploy, 'check_health', side_effect=RuntimeError), patch.object(deploy.time, 'sleep'):
            with self.assertRaisesRegex(SystemExit, 'Manual application rollback'):
                deploy.main()
        self.assertEqual(json.loads(self.state.read_text()), self.previous)
        self.assertFalse((self.root / 'cd/active-images.json').exists())

    def test_mutable_image_reference_is_rejected_before_docker_or_backup(self):
        self.release['backend'] = 'ghcr.io/jbroich/homedashboard-backend:latest'
        self.manifest.write_text(json.dumps(self.release))
        with patch.object(deploy, 'run') as run:
            with self.assertRaises(SystemExit):
                deploy.main()
        run.assert_not_called()

    def test_health_requires_up_json_even_when_http_is_successful(self):
        response = io.BytesIO(b'{"status":"DOWN"}')
        response.status = 200
        with patch.object(deploy.urllib.request, 'build_opener') as opener:
            opener.return_value.open.return_value = response
            with self.assertRaisesRegex(RuntimeError, 'database is unhealthy'):
                deploy.check_health('http://127.0.0.1:8081/actuator/health', 'http://127.0.0.1:8080/')

    def test_already_deployed_release_does_not_repeat_backup_or_update(self):
        self.state.write_text(json.dumps(self.release))
        with patch.object(deploy, 'run') as run:
            deploy.main()
        run.assert_not_called()


if __name__ == '__main__':
    unittest.main()
