#!/usr/bin/env bash
set -euo pipefail

PACKAGE_TYPE="${1:-app-image}"
case "$PACKAGE_TYPE" in
  app-image|deb|rpm) ;;
  *)
    echo "Tipo inválido: $PACKAGE_TYPE. Use app-image, deb ou rpm." >&2
    exit 1
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_DIR="$PROJECT_DIR/target"
INPUT_DIR="$TARGET_DIR/jpackage-input"
DESTINATION_DIR="$TARGET_DIR/installer"

cd "$PROJECT_DIR"
./mvnw clean package dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="$INPUT_DIR"

cp "$TARGET_DIR/gaspar-desktop-1.0.0.jar" "$INPUT_DIR/"
mkdir -p "$DESTINATION_DIR"

COMMON_ARGS=(
  --type "$PACKAGE_TYPE"
  --name Gaspar
  --app-version 1.0.0
  --vendor Gaspar
  --description "Conferência Domínio e SPED"
  --input "$INPUT_DIR"
  --main-jar gaspar-desktop-1.0.0.jar
  --main-class br.com.gaspar.Launcher
  --icon "$PROJECT_DIR/src/main/resources/icons/gaspar.png"
  --dest "$DESTINATION_DIR"
)

if [[ "$PACKAGE_TYPE" == "deb" || "$PACKAGE_TYPE" == "rpm" ]]; then
  COMMON_ARGS+=(--linux-shortcut --linux-menu-group "Escritório")
fi

jpackage "${COMMON_ARGS[@]}"

if [[ "$PACKAGE_TYPE" == "app-image" ]]; then
  ARCHIVE="$DESTINATION_DIR/Gaspar-linux-$(uname -m).tar.gz"
  tar -czf "$ARCHIVE" -C "$DESTINATION_DIR" Gaspar
  echo "Aplicação portátil criada em $ARCHIVE"
else
  echo "Pacote $PACKAGE_TYPE criado em $DESTINATION_DIR"
fi
