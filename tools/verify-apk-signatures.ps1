param(
    [Parameter(Mandatory=$true)]
    [string]$Apk
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Apk)) {
    throw "APK not found: $Apk"
}

$Sdk = $env:ANDROID_SDK_ROOT
if (-not $Sdk) {
    $Sdk = $env:ANDROID_HOME
}
if (-not $Sdk) {
    throw "ANDROID_SDK_ROOT / ANDROID_HOME is not configured."
}

$BuildToolsRoot = Join-Path $Sdk "build-tools"
$BuildTools = Get-ChildItem $BuildToolsRoot -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1

if (-not $BuildTools) {
    throw "Android SDK build-tools not found."
}

$ApkSigner = Join-Path $BuildTools.FullName "apksigner.bat"
if (-not (Test-Path $ApkSigner)) {
    $ApkSigner = Join-Path $BuildTools.FullName "apksigner"
}

$Output = & $ApkSigner verify --verbose --print-certs $Apk 2>&1
$SignatureExit = $LASTEXITCODE
$Output | Write-Host
if ($SignatureExit -ne 0) { throw "apksigner failed with exit code $SignatureExit" }

$Required = @(
    "Verified using v3 scheme (APK Signature Scheme v3): true"
)

foreach ($Line in $Required) {
    if (-not ($Output -match [regex]::Escape($Line))) {
        throw "Required APK signature scheme missing: $Line"
    }
}

Write-Host ""
Write-Host "OK: APK v3 signature verified (minSdk 32; v1/v2/v4 disabled)."
