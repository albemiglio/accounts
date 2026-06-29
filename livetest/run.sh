#!/usr/bin/env bash
# Runs a REAL end-to-end UUID migration against the dockerized MySQL + Redis (see docker-compose.yml),
# then asserts the SQL row and the EssentialsX file are now keyed by the new UUID. Exits non-zero if not.
#
#   cd livetest && docker compose up -d && ./run.sh        # full dockerized run
#   LIVETEST_DB=sqlite ./run.sh                            # offline proof, no Docker needed
#   ./run.sh --down                                        # tear the stack down
#
# Needs a JDK 17 to build (the project targets Java 8 bytecode); set JAVA_HOME if java is older.
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd .. && pwd)"

if [[ "${1:-}" == "--down" ]]; then
    docker compose down -v
    exit 0
fi

# Build core + the harness via the opt-in `live` profile (default build never sees this module).
echo "==> Building engine + harness (mvn -Plive)"
( cd "$ROOT" && mvn -q -Plive -pl core,livetest -am install -DskipTests )

# Fresh EssentialsX fixture each run, so a rerun (where the file is already renamed) still starts at OLD.
echo "==> Seeding EssentialsX userdata fixture"
rm -rf fixtures/userdata
mkdir -p fixtures/userdata
cp seed/essentialsx-userdata.yml \
   "fixtures/userdata/069a79f4-44e9-4726-a5be-fca90e38aaf5.yml"

if [[ "${LIVETEST_DB:-}" != "sqlite" ]]; then
    echo "==> Waiting for MySQL + Redis to be healthy"
    for i in $(seq 1 40); do
        up=$(docker compose ps --format '{{.Health}}' 2>/dev/null | grep -c healthy || true)
        [[ "$up" -ge 2 ]] && break
        sleep 2
    done
    docker compose ps
fi

echo "==> Running migration + assertions"
( cd "$ROOT" && mvn -q -Plive -pl livetest exec:java \
    -Dexec.args="" \
    -Dlivetest.dir="$ROOT/livetest" )
