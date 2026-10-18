import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap
import unittest


class ManifestProducerTest(unittest.TestCase):
    def generate(self, backend_digest):
        workflow = (Path(__file__).parents[3] / '.github/workflows/build-and-push.yml').read_text()
        release_job = workflow.split('  release-manifest:\n', 1)[1]
        script = release_job.split("python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0]
        settings = dict(os.environ, COMMIT='a' * 40, REPOSITORY='jbroich/homedashboard', RUN_ID='123', BACKEND_DIGEST=backend_digest, FRONTEND_DIGEST='sha256:' + 'c' * 64, BACKEND_IMAGE_NAME='jbroich/homedashboard-backend', FRONTEND_IMAGE_NAME='jbroich/homedashboard-frontend')
        with tempfile.TemporaryDirectory() as temporary:
            subprocess.run([sys.executable, '-c', textwrap.dedent(script)], cwd=temporary, env=settings, check=True, capture_output=True)
            return json.loads((Path(temporary) / 'release.json').read_text())

    def test_workflow_generates_one_manifest_with_both_build_outputs(self):
        release = self.generate('sha256:' + 'b' * 64)
        self.assertEqual(release['commit'], 'a' * 40)
        self.assertEqual(release['run_id'], 123)
        self.assertEqual(release['backend'], 'ghcr.io/jbroich/homedashboard-backend@sha256:' + 'b' * 64)
        self.assertEqual(release['frontend'], 'ghcr.io/jbroich/homedashboard-frontend@sha256:' + 'c' * 64)

    def test_workflow_refuses_to_publish_when_a_digest_is_missing(self):
        with self.assertRaises(subprocess.CalledProcessError):
            self.generate('')


if __name__ == '__main__':
    unittest.main()
