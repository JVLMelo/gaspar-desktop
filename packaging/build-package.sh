#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$(uname -s)" in
  Linux)
    exec "$SCRIPT_DIR/linux/build-package.sh" "$@"
    ;;
  Darwin)
    exec "$SCRIPT_DIR/macos/build-package.sh" "$@"
    ;;
  *)
    echo "Sistema não suportado por este script. No Windows, use packaging\\windows\\build-installer.ps1." >&2
    exit 1
    ;;
esac
