#!/usr/bin/env python3
"""Verify the latest successful main build through GitHub, optionally deploy it."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile

REPOSITORY = 'jbroich/homedashboard'
WORKFLOW = '.github/workflows/build-and-push.yml'
ARTIFACT = 'homedashboard-release'


def api(path):
    result = subprocess.run([
        'gh', 'api', '--hostname', 'github.com', 'repos/' + REPOSITORY + '/' + path,
    ], check=True, capture_output=True, text=True, timeout=60)
    return json.loads(result.stdout)


def verified_release(directory):
    runs = api('actions/workflows/build-and-push.yml/runs?branch=main&event=push&status=success&per_page=1')['workflow_runs']
    if not runs:
        raise ValueError('No successful main build available')
    run = runs[0]
    if (run['event'], run['head_branch'], run['conclusion'], run['path'], run['head_repository']['full_name']) != ('push', 'main', 'success', WORKFLOW, REPOSITORY):
        raise ValueError('Unexpected workflow provenance')
    run_id = int(run['id'])
    # Require every expected job, including the release producer, to have succeeded.
    successful = set()
    for page in range(1, 11):
        result = api(f'actions/runs/{run_id}/jobs?per_page=100&page={page}')
        successful.update(job['name'] for job in result['jobs'] if job['conclusion'] == 'success')
        if len(result['jobs']) < 100:
            break
    required = {'backend-checks', 'frontend-checks', 'backend-image', 'frontend-image', 'release-manifest'}
    if not required.issubset(successful):
        raise ValueError('Required checks, image builds, or release publication did not succeed')
    deployment_runs = api('actions/workflows/deployment-checks.yml/runs?branch=main&event=push&status=success&per_page=1&head_sha=' + run['head_sha'])['workflow_runs']
    if not any(
        item['head_sha'] == run['head_sha']
        and item['event'] == 'push' and item['head_branch'] == 'main'
        and item['conclusion'] == 'success'
        and item['path'] == '.github/workflows/deployment-checks.yml'
        and item['head_repository']['full_name'] == REPOSITORY
        for item in deployment_runs
    ):
        raise ValueError('Deployment and restore checks have not succeeded for this commit')
    artifacts = api(f'actions/runs/{run_id}/artifacts?name={ARTIFACT}&per_page=100')['artifacts']
    matching = [item for item in artifacts if item['name'] == ARTIFACT and not item['expired']]
    if len(matching) != 1 or matching[0]['workflow_run']['head_sha'] != run['head_sha']:
        raise ValueError('No unambiguous release artifact for the verified commit')
    subprocess.run([
        'gh', 'run', 'download', str(run_id), '--repo', 'github.com/' + REPOSITORY,
        '--name', ARTIFACT, '--dir', str(directory),
    ], check=True, timeout=120)
    manifest = directory / 'release.json'
    if manifest.stat().st_size > 16384:
        raise ValueError('Release manifest is unexpectedly large')
    release = json.loads(manifest.read_text())
    if release.get('repository') != REPOSITORY or release.get('run_id') != run_id or release.get('commit') != run['head_sha']:
        raise ValueError('Release manifest does not match verified workflow run')
    if not re.fullmatch(r'[0-9a-f]{40}', release['commit']):
        raise ValueError('Invalid release commit')
    for service in ('backend', 'frontend'):
        if not re.fullmatch(r'ghcr\.io/jbroich/homedashboard-' + service + r'@sha256:[0-9a-f]{64}', release.get(service, '')):
            raise ValueError('Invalid release image reference')
    return release


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--deploy', action='store_true', help='Deploy the verified manifest; default only reports it')
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='homedashboard-release-') as temporary:
        directory = Path(temporary)
        release = verified_release(directory)
        if args.deploy:
            # Hand deploy.py the validated object, not an independently supplied file.
            manifest = directory / 'verified-release.json'
            manifest.write_text(json.dumps(release))
            subprocess.run([sys.executable, str(Path(__file__).with_name('deploy.py')), str(manifest)], check=True)
        else:
            print(json.dumps(release, indent=2))


if __name__ == '__main__':
    main()
