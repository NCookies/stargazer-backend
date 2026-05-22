#requires -Version 5.1
<#
.SYNOPSIS
  이 디렉터리의 Terraform 상태에 따라 AWS 리소스를 모두 삭제합니다.
  기존 *.tf 스크립트는 수정·삭제하지 않습니다.

.DESCRIPTION
  - 실행 전반은 infra 디렉터리에서 terraform destroy 를 수행합니다.
  - terraform.tfvars 가 같은 폴더에 있으면 자동으로 -var-file 로 넘깁니다.
  - Terraform 밖에서 만든 리소스(수동 생성 등)는 이 스크립트로 지워지지 않습니다.

.PARAMETER AutoApprove
  확인 없이 바로 삭제합니다. 생략 시 terraform 이 한 번 더 확인을 요청합니다.

.PARAMETER VarFile
  변수 파일 경로. 생략 시 같은 폴더의 terraform.tfvars 가 있으면 사용합니다.

.PARAMETER SkipInit
  terraform init 을 건너뜁니다. 이미 init 된 환경에서만 사용하세요.
#>
param(
    [switch]$AutoApprove,
    [string]$VarFile = "",
    [switch]$SkipInit
)

$ErrorActionPreference = "Stop"
$InfraRoot = $PSScriptRoot

Write-Host ""
Write-Host "=== Stargazer AWS 리소스 해제 (Terraform destroy) ===" -ForegroundColor Yellow
Write-Host "디렉터리: $InfraRoot"
Write-Host "이 작업은 되돌릴 수 없습니다. 상태 파일(terraform.tfstate)에 등록된 리소스가 삭제됩니다."
if (-not $AutoApprove) {
    Write-Host "-AutoApprove 없이 실행하면 terraform 이 마지막으로 한 번 더 확인합니다." -ForegroundColor DarkGray
}
Write-Host ""

Push-Location $InfraRoot
try {
    if (-not $SkipInit) {
        terraform init
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    $tfArgs = @("destroy")
    if ($AutoApprove) {
        $tfArgs += "-auto-approve"
    }

    $resolvedVarFile = $VarFile
    if (-not $resolvedVarFile) {
        $defaultTfvars = Join-Path $InfraRoot "terraform.tfvars"
        if (Test-Path $defaultTfvars) {
            $resolvedVarFile = $defaultTfvars
        }
    }
    if ($resolvedVarFile) {
        if (-not (Test-Path $resolvedVarFile)) {
            throw "VarFile 을 찾을 수 없습니다: $resolvedVarFile"
        }
        $tfArgs += "-var-file=$resolvedVarFile"
        Write-Host "변수 파일: $resolvedVarFile"
    }
    else {
        Write-Host "terraform.tfvars 없음 — TF_VAR_* 환경 변수 등으로 변수를 제공해야 할 수 있습니다." -ForegroundColor DarkYellow
    }

    Write-Host ""
    terraform @tfArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
