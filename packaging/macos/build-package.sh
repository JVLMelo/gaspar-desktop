#!/usr/bin/env bash
set -euo pipefail

PACKAGE_TYPE="${1:-dmg}"
case "$PACKAGE_TYPE" in
  app-image|dmg|pkg) ;;
  *)
    echo "Tipo inválido: $PACKAGE_TYPE. Use app-image, dmg ou pkg." >&2
    exit 1
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET_DIR="$PROJECT_DIR/target"
INPUT_DIR="$TARGET_DIR/jpackage-input"
DESTINATION_DIR="$TARGET_DIR/installer"
APP_VERSION="$(sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' "$PROJECT_DIR/pom.xml" | head -n 1)"
if [[ -z "$APP_VERSION" ]]; then
  echo "Não foi possível encontrar a versão no pom.xml." >&2
  exit 1
fi

cd "$PROJECT_DIR"
./mvnw clean package dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="$INPUT_DIR"

cp "$TARGET_DIR/gaspar-desktop-$APP_VERSION.jar" "$INPUT_DIR/"
mkdir -p "$DESTINATION_DIR"

jpackage \
  --type "$PACKAGE_TYPE" \
  --name Gaspar \
  --app-version "$APP_VERSION" \
  --vendor Gaspar \
  --description "Conferência Domínio e SPED" \
  --input "$INPUT_DIR" \
  --main-jar "gaspar-desktop-$APP_VERSION.jar" \
  --main-class br.com.gaspar.Launcher \
  --icon "$PROJECT_DIR/src/main/resources/icons/gaspar.icns" \
  --dest "$DESTINATION_DIR" \
  --mac-package-name Gaspar

if [[ "$PACKAGE_TYPE" == "app-image" ]]; then
  ARCHIVE="$DESTINATION_DIR/Gaspar-macos-$(uname -m).tar.gz"
  tar -czf "$ARCHIVE" -C "$DESTINATION_DIR" Gaspar.app
  echo "Aplicação portátil criada em $ARCHIVE"
else
  echo "Pacote $PACKAGE_TYPE criado em $DESTINATION_DIR"
fi
