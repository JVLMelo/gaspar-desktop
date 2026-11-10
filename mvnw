#!/usr/bin/env sh
set -eu

MAVEN_VERSION=3.9.11
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  if [ ! -f "$ARCHIVE" ]; then
    curl -fL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" -o "$ARCHIVE"
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
