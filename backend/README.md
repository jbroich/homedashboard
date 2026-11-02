# Backend tests

Run the complete backend test suite from this directory:

```powershell
.\mvnw.cmd test
```

Java 21 and a running Docker engine with Linux-container support are required.
On Windows, start Docker Desktop first. Testcontainers starts an ephemeral
`postgres:15` container on a dynamically assigned port; no local PostgreSQL
instance or deployment `.env` file is needed. The container is removed after
the test run. The first run needs network access to download the image.

`MeasurementRepositoryTest` exercises entity persistence, latest-measurement
lookup, inclusive time-range filtering, and PostgreSQL hourly/daily aggregation.
Its database session uses UTC so bucket assertions are independent of the host
time zone. Other tests use mocks or the isolated H2 application context. MQTT
is disabled in tests, so no broker is needed.

For a quick run without the container-backed repository tests:

```powershell
.\mvnw.cmd "-Dtest=!MeasurementRepositoryTest" test
```

Run the complete suite before merging. CI runs it on a Docker-capable runner
before publishing the backend image. The Dockerfile skips test execution because
Docker image build stages do not provide the Docker engine needed by
Testcontainers.

To run the tests and build the executable Spring Boot package:

```powershell
.\mvnw.cmd --batch-mode verify
```

The resulting package is `target/homedashboard-0.0.1-SNAPSHOT.jar`. CI runs the
same verification on pull requests and `main`, and uploads the executable JAR
and test reports as commit-specific workflow artifacts retained for 14 days.
The frontend checks also export and upload a static web package. Both ARM64
images are built after the checks succeed, including on pull requests. Images
are published to GHCR only on non-pull-request runs; each published image
receives the full commit SHA tag as well as `latest`.
