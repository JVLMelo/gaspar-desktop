param(
  [ValidateSet("msi", "exe", "app-image")]
  [string]$PackageType = "msi"
)

$ErrorActionPreference = "Stop"

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$TargetDir = Join-Path $ProjectDir "target"
$InputDir = Join-Path $TargetDir "jpackage-input"
$DestinationDir = Join-Path $TargetDir "installer"
$PomContent = Get-Content (Join-Path $ProjectDir "pom.xml") -Raw
$VersionMatch = [regex]::Match($PomContent, '<version>(?<version>[^<]+)</version>')
if (-not $VersionMatch.Success) { throw "Não foi possível encontrar a versão no pom.xml." }
$AppVersion = $VersionMatch.Groups["version"].Value
if ($env:GITHUB_REF_TYPE -eq "tag" -and $env:GITHUB_REF_NAME -ne "v$AppVersion") {
  throw "A tag $env:GITHUB_REF_NAME não corresponde à versão $AppVersion do pom.xml."
}
$UpgradeUuid = "7A0B6FB6-0D94-4E45-9281-2ABF5F18B637"

Set-Location $ProjectDir
& .\mvnw.cmd clean package dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$InputDir"
if ($LASTEXITCODE -ne 0) { throw "Falha na compilação Maven." }

Copy-Item (Join-Path $TargetDir "gaspar-desktop-$AppVersion.jar") $InputDir -Force
New-Item -ItemType Directory -Force -Path $DestinationDir | Out-Null

$JpackageArgs = @(
  "--type", $PackageType,
  "--name", "Gaspar",
  "--app-version", $AppVersion,
  "--vendor", "Gaspar",
  "--description", "Conferência Domínio e SPED",
  "--input", $InputDir,
  "--main-jar", "gaspar-desktop-$AppVersion.jar",
  "--main-class", "br.com.gaspar.Launcher",
  "--icon", (Join-Path $ProjectDir "src\main\resources\icons\gaspar.ico"),
  "--dest", $DestinationDir
)

if ($PackageType -ne "app-image") {
  $JpackageArgs += @("--win-menu", "--win-shortcut", "--win-dir-chooser")
  $JpackageArgs += @("--win-upgrade-uuid", $UpgradeUuid)
}

& jpackage @JpackageArgs

if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar o instalador com jpackage." }
Write-Host "Pacote $PackageType criado em $DestinationDir"
