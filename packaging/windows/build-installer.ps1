param(
  [ValidateSet("msi", "exe", "app-image")]
  [string]$PackageType = "msi"
)

$ErrorActionPreference = "Stop"

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$TargetDir = Join-Path $ProjectDir "target"
$InputDir = Join-Path $TargetDir "jpackage-input"
$DestinationDir = Join-Path $TargetDir "installer"

Set-Location $ProjectDir
& .\mvnw.cmd clean package dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$InputDir"
if ($LASTEXITCODE -ne 0) { throw "Falha na compilação Maven." }

Copy-Item (Join-Path $TargetDir "gaspar-desktop-1.0.0.jar") $InputDir -Force
New-Item -ItemType Directory -Force -Path $DestinationDir | Out-Null

$JpackageArgs = @(
  "--type", $PackageType,
  "--name", "Gaspar",
  "--app-version", "1.0.0",
  "--vendor", "Gaspar",
  "--description", "Conferência Domínio e SPED",
  "--input", $InputDir,
  "--main-jar", "gaspar-desktop-1.0.0.jar",
  "--main-class", "br.com.gaspar.Launcher",
  "--icon", (Join-Path $ProjectDir "src\main\resources\icons\gaspar.ico"),
  "--dest", $DestinationDir
)

if ($PackageType -ne "app-image") {
  $JpackageArgs += @("--win-menu", "--win-shortcut", "--win-dir-chooser")
}

& jpackage @JpackageArgs

if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar o instalador com jpackage." }
Write-Host "Pacote $PackageType criado em $DestinationDir"
