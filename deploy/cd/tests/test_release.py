import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('poll_release', Path(__file__).parents[1] / 'poll-release.py')
poll = importlib.util.module_from_spec(spec)
spec.loader.exec_module(poll)


class ReleaseProvenanceTest(unittest.TestCase):
    def setUp(self):
        self.run = {'id': 123, 'head_sha': 'a' * 40, 'event': 'push', 'head_branch': 'main', 'conclusion': 'success', 'path': poll.WORKFLOW, 'head_repository': {'full_name': poll.REPOSITORY}}
        self.release = {'commit': self.run['head_sha'], 'repository': poll.REPOSITORY, 'run_id': 123, 'backend': 'ghcr.io/jbroich/homedashboard-backend@sha256:' + 'b' * 64, 'frontend': 'ghcr.io/jbroich/homedashboard-frontend@sha256:' + 'c' * 64}
        self.jobs = {'jobs': [{'name': name, 'conclusion': 'success'} for name in ('backend-checks', 'frontend-checks', 'backend-image', 'frontend-image', 'release-manifest')]}
        self.artifacts = {'artifacts': [{'name': poll.ARTIFACT, 'expired': False, 'workflow_run': {'head_sha': self.run['head_sha']}}]}
        self.deployment_runs = [dict(self.run, path='.github/workflows/deployment-checks.yml')]

    def fetch(self, path):
        if path.startswith('actions/workflows/deployment-checks.yml/'):
            return {'workflow_runs': self.deployment_runs}
        if '/runs?' in path:
            return {'workflow_runs': [self.run]}
        if '/jobs?' in path:
            return self.jobs
        if '/artifacts?' in path:
            return self.artifacts
        self.fail('Unexpected API call: ' + path)

    def verify(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            def download(*args, **kwargs):
                (directory / 'release.json').write_text(json.dumps(self.release))
            with patch.object(poll, 'api', side_effect=self.fetch), patch.object(poll.subprocess, 'run', side_effect=download):
                return poll.verified_release(directory)

    def test_accepts_matching_images_from_one_verified_successful_run(self):
        self.assertEqual(self.verify(), self.release)

    def test_rejects_pull_request_even_if_api_returned_it(self):
        self.run['event'] = 'pull_request'
        with self.assertRaisesRegex(ValueError, 'workflow provenance'):
            self.verify()

    def test_rejects_skipped_backend_checks(self):
        self.jobs['jobs'][0]['conclusion'] = 'skipped'
        with self.assertRaisesRegex(ValueError, 'Required checks'):
            self.verify()

    def test_rejects_manifest_from_another_commit(self):
        self.release['commit'] = 'd' * 40
        with self.assertRaisesRegex(ValueError, 'does not match'):
            self.verify()

    def test_rejects_expired_artifacts(self):
        self.artifacts['artifacts'][0]['expired'] = True
        with self.assertRaisesRegex(ValueError, 'release artifact'):
            self.verify()

    def test_rejects_release_until_separate_deployment_checks_succeed(self):
        self.deployment_runs = []
        with self.assertRaisesRegex(ValueError, 'Deployment and restore checks'):
            self.verify()


if __name__ == '__main__':
    unittest.main()
