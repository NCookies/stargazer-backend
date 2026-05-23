#!/usr/bin/env bash
#
# 이 디렉터리의 Terraform 상태에 따라 AWS 리소스를 모두 삭제합니다.
# 기존 *.tf 스크립트는 수정·삭제하지 않습니다.
#
# 사용법:
#   ./destroy-aws.sh              # 확인 후 삭제 (terraform.tfvars 자동 사용)
#   ./destroy-aws.sh --yes        # -auto-approve (바로 삭제)
#   ./destroy-aws.sh --var-file=/path/to.tfvars
#   ./destroy-aws.sh --skip-init  # terraform init 생략
#
set -euo pipefail

INFRA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTO_APPROVE=0
SKIP_INIT=0
VAR_FILE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes|-y)
      AUTO_APPROVE=1
      shift
      ;;
    --skip-init)
      SKIP_INIT=1
      shift
      ;;
    --var-file=*)
      VAR_FILE="${1#*=}"
      shift
      ;;
    --var-file)
      VAR_FILE="$2"
      shift 2
      ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
    *)
      echo "알 수 없는 인자: $1" >&2
      exit 1
      ;;
  esac
done

echo ""
echo "=== Stargazer AWS 리소스 해제 (Terraform destroy) ==="
echo "디렉터리: $INFRA_ROOT"
echo "이 작업은 되돌릴 수 없습니다."
if [[ "$AUTO_APPROVE" -eq 0 ]]; then
  echo "--yes 없이 실행하면 terraform 이 마지막으로 한 번 더 확인합니다."
fi
echo ""

cd "$INFRA_ROOT"

if [[ "$SKIP_INIT" -eq 0 ]]; then
  terraform init
fi

TF_ARGS=(destroy)
if [[ "$AUTO_APPROVE" -eq 1 ]]; then
  TF_ARGS+=(-auto-approve)
fi

if [[ -z "$VAR_FILE" && -f "$INFRA_ROOT/terraform.tfvars" ]]; then
  VAR_FILE="$INFRA_ROOT/terraform.tfvars"
fi

if [[ -n "$VAR_FILE" ]]; then
  if [[ ! -f "$VAR_FILE" ]]; then
    echo "VarFile 을 찾을 수 없습니다: $VAR_FILE" >&2
    exit 1
  fi
  TF_ARGS+=("-var-file=$VAR_FILE")
  echo "변수 파일: $VAR_FILE"
else
  echo "terraform.tfvars 없음 — TF_VAR_* 등으로 변수를 제공해야 할 수 있습니다." >&2
fi

echo ""
terraform "${TF_ARGS[@]}"
