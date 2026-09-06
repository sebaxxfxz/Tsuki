#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

DETEKT_VERSION="1.23.8"
JAR="${HOME}/.cache/tsuki/detekt-cli-${DETEKT_VERSION}-all.jar"
JDK21="${HOME}/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2/bin/java"

mkdir -p "${HOME}/.cache/tsuki" build/reports/detekt

if [ ! -f "$JAR" ]; then
    curl -sL -o "$JAR" "https://repo1.maven.org/maven2/io/gitlab/arturbosch/detekt/detekt-cli/${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}-all.jar"
fi

if [ ! -x "$JDK21" ]; then
    JDK21="java"
fi

"$JDK21" -jar "$JAR" \
    --input app/src/main/java \
    --config config/detekt/detekt.yml \
    --build-upon-default-config \
    --jvm-target 21 \
    --report txt:build/reports/detekt/report.txt \
    --report html:build/reports/detekt/report.html

echo "Reportes en build/reports/detekt/"
