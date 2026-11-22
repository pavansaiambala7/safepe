#!/usr/bin/env sh
# Runs the backend tests inside a Java 17 container, so no local JDK or Maven
# is needed (only Docker). Extra arguments go to Maven, for example:
#   scripts/test.sh                          # all services
#   scripts/test.sh -pl fraud-service        # one service
#   scripts/test.sh -pl payment-service -Dtest=PaymentServiceTest
#
# The Docker socket is shared so Testcontainers can start PostgreSQL and Kafka
# as sibling containers; host.docker.internal lets the tests reach them.
set -e
cd "$(dirname "$0")/.."

ROOT="$(pwd -W 2>/dev/null || pwd)"   # pwd -W gives a Windows path under Git Bash

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$ROOT:/src" \
  -v safepe-m2:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  --add-host=host.docker.internal:host-gateway \
  -w /src \
  eclipse-temurin:17-jdk \
  sh ./mvnw -B clean verify "$@"

echo "Coverage reports: <service>/target/site/jacoco/index.html"
