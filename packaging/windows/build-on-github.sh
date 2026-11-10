#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
WORKFLOW="windows-exe.yml"
OUTPUT_DIR="${1:-$PROJECT_DIR/target/windows-exe}"

if ! command -v gh >/dev/null 2>&1; then
  echo "O GitHub CLI (gh) não está instalado." >&2
  echo "Instale-o, execute 'gh auth login' e tente novamente." >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "O GitHub CLI ainda não está autenticado. Execute: gh auth login" >&2
  exit 1
fi

cd "$PROJECT_DIR"

BRANCH="$(git -C "$PROJECT_DIR" branch --show-current)"
if [[ -z "$BRANCH" ]]; then
  echo "Não foi possível identificar a branch atual." >&2
  exit 1
fi

if ! git -C "$PROJECT_DIR" diff --quiet || ! git -C "$PROJECT_DIR" diff --cached --quiet; then
  echo "Existem alterações locais ainda não enviadas ao GitHub." >&2
  echo "Faça commit e push antes de gerar o EXE para que ele contenha essas alterações." >&2
  exit 1
fi

PREVIOUS_RUN="$(gh run list --workflow "$WORKFLOW" --branch "$BRANCH" \
  --event workflow_dispatch --limit 1 --json databaseId --jq '.[0].databaseId // empty')"

echo "Solicitando a geração do EXE na branch '$BRANCH'..."
gh workflow run "$WORKFLOW" --ref "$BRANCH"

RUN_ID=""
for _ in {1..20}; do
  RUN_ID="$(gh run list --workflow "$WORKFLOW" --branch "$BRANCH" \
    --event workflow_dispatch --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
  if [[ -n "$RUN_ID" && "$RUN_ID" != "$PREVIOUS_RUN" ]]; then
    break
  fi
  sleep 2
done

if [[ -z "$RUN_ID" || "$RUN_ID" == "$PREVIOUS_RUN" ]]; then
  echo "O workflow foi solicitado, mas a execução ainda não apareceu no GitHub." >&2
  echo "Acompanhe manualmente em: https://github.com/JVLMelo/gaspar-desktop/actions" >&2
  exit 1
fi

echo "Acompanhando a execução $RUN_ID..."
gh run watch "$RUN_ID" --exit-status

mkdir -p "$OUTPUT_DIR"
gh run download "$RUN_ID" --name "Gaspar-Windows-$RUN_ID" --dir "$OUTPUT_DIR"

echo "Instalador baixado em: $OUTPUT_DIR"

