Param(
    [Parameter(Mandatory=$true)][string]$SourceProjectPath,
    [string]$DestProjectPath = "C:\Users\ivanm\AndroidStudioProjects\RoveCast"
)

# === Config ===
$OldPkg = "com.ivanmarty.radiola"
$NewPkg = "com.ivanmarty.rovecast"

$TestIds = @{
  APP_ID                = "ca-app-pub-3940256099942544~3347511713"
  BANNER                = "ca-app-pub-3940256099942544/6300978111"
  INTERSTITIAL          = "ca-app-pub-3940256099942544/1033173712"
  REWARDED              = "ca-app-pub-3940256099942544/5224354917"
  REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
  NATIVE                = "ca-app-pub-3940256099942544/2247696110"
  APPOPEN               = "ca-app-pub-3940256099942544/3419835294"
}

$Extensions = @(".java",".kt",".xml",".gradle",".md",".txt",".pro",".properties",".json",".yml",".yaml",".cfg",".html",".css",".js",".gitignore")

Write-Host ">> Copiando proyecto a: $DestProjectPath"
if (Test-Path $DestProjectPath) { Remove-Item -Recurse -Force $DestProjectPath }
Copy-Item -Recurse -Force -LiteralPath $SourceProjectPath -Destination $DestProjectPath

# Función para reemplazar IDs de AdMob con contexto por línea
function Replace-AdUnitIdsByContext([string]$content) {
  $lines = $content -split "`r?`n"
  for ($i=0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $low  = $line.ToLower()
    $pattern = 'ca-app-pub-\d{16}/\d+'
    if ([Text.RegularExpressions.Regex]::IsMatch($line, $pattern)) {
      $replacement = if ($low -match 'banner|adview|ad_view') {
        $TestIds.BANNER
      } elseif ($low -match 'rewarded_interstitial|rewardedinterstitial') {
        $TestIds.REWARDED_INTERSTITIAL
      } elseif ($low -match 'rewarded') {
        $TestIds.REWARDED
      } elseif ($low -match 'native') {
        $TestIds.NATIVE
      } elseif ($low -match 'appopen|app_open|appopenad|openad|app open') {
        $TestIds.APPOPEN
      } elseif ($low -match 'interstitial|fullscreencontent|show') {
        $TestIds.INTERSTITIAL
      } else {
        $TestIds.INTERSTITIAL
      }
      $lines[$i] = [Text.RegularExpressions.Regex]::Replace($line, $pattern, $replacement)
    }
  }
  return ($lines -join "`r`n")
}

# Reemplazos en archivos de texto
$files = Get-ChildItem -Path $DestProjectPath -Recurse -Force -File | Where-Object { $Extensions -contains $_.Extension.ToLower() }
$changed = 0
foreach ($f in $files) {
  $content = Get-Content -LiteralPath $f.FullName -Encoding UTF8 -Raw

  $orig = $content

  # Paquete
  $content = $content.Replace($OldPkg, $NewPkg)

  # Nombre y marcas
  $content = $content.Replace("Radiola","RoveCast").Replace("radiola","rovecast").Replace("RADIOLA","ROVECAST")

  # App ID
  $content = [Text.RegularExpressions.Regex]::Replace($content, 'ca-app-pub-\d{16}~\d+', $TestIds.APP_ID)

  # Unit IDs contextuales
  $content = Replace-AdUnitIdsByContext $content

  if ($content -ne $orig) {
    Set-Content -LiteralPath $f.FullName -Value $content -Encoding UTF8
    $changed++
  }
}

# Mover paquete de código Java/Kotlin
$oldPath = Join-Path $DestProjectPath ("app\src\main\java\" + ($OldPkg -replace '\.', '\'))
$newPath = Join-Path $DestProjectPath ("app\src\main\java\" + ($NewPkg -replace '\.', '\'))

if (Test-Path $oldPath) {
  New-Item -ItemType Directory -Force -Path (Split-Path $newPath) | Out-Null
  if (Test-Path $newPath) {
    Get-ChildItem -Recurse -Force $oldPath | ForEach-Object {
      $rel = $_.FullName.Substring($oldPath.Length).TrimStart('\')
      $dest = Join-Path $newPath $rel
      if ($_.PSIsContainer) {
        New-Item -ItemType Directory -Force -Path $dest | Out-Null
      } else {
        New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
        Move-Item -Force -LiteralPath $_.FullName -Destination $dest
      }
    }
    Remove-Item -Recurse -Force $oldPath
  } else {
    New-Item -ItemType Directory -Force -Path (Split-Path $newPath) | Out-Null
    Move-Item -Force -LiteralPath (Split-Path $oldPath) -Destination (Split-Path $newPath)
  }
}

# settings.gradle -> rootProject.name = "RoveCast"
$settings = Join-Path $DestProjectPath "settings.gradle"
if (Test-Path $settings) {
  $s = Get-Content -LiteralPath $settings -Encoding UTF8 -Raw
  $s2 = [Text.RegularExpressions.Regex]::Replace($s, 'rootProject\.name\s*=\s*["''].*?["'']', 'rootProject.name = "RoveCast"')
  if ($s2 -ne $s) { Set-Content -LiteralPath $settings -Value $s2 -Encoding UTF8 }
}

# app/build.gradle -> applicationId & namespace
$appGradle = Join-Path $DestProjectPath "app\build.gradle"
if (Test-Path $appGradle) {
  $g = Get-Content -LiteralPath $appGradle -Encoding UTF8 -Raw
  $g = [Text.RegularExpressions.Regex]::Replace($g, 'applicationId\s+"[^"]+"', "applicationId `"$NewPkg`"")
  $g = [Text.RegularExpressions.Regex]::Replace($g, 'namespace\s+"[^"]+"', "namespace `"$NewPkg`"")
  Set-Content -LiteralPath $appGradle -Value $g -Encoding UTF8
}

# strings.xml -> app_name = RoveCast
$strings = Join-Path $DestProjectPath "app\src\main\res\values\strings.xml"
if (Test-Path $strings) {
  $st = Get-Content -LiteralPath $strings -Encoding UTF8 -Raw
  $st2 = [Text.RegularExpressions.Regex]::Replace($st, '(<string\s+name="app_name"\s*>\s*)(.*?)(\s*</string>)', '$1RoveCast$3', 'Singleline')
  if ($st2 -ne $st) { Set-Content -LiteralPath $strings -Value $st2 -Encoding UTF8 }
}

# Manifest: label -> @string/app_name y authorities/paquete a nuevo pkg
$manifest = Join-Path $DestProjectPath "app\src\main\AndroidManifest.xml"
if (Test-Path $manifest) {
  $mt = Get-Content -LiteralPath $manifest -Encoding UTF8 -Raw
  $mt = [Text.RegularExpressions.Regex]::Replace($mt, 'android:label\s*=\s*"[^\"]+"', 'android:label="@string/app_name"')
  $mt = $mt.Replace($OldPkg, $NewPkg)
  Set-Content -LiteralPath $manifest -Value $mt -Encoding UTF8
}

Write-Host ">> Archivos modificados: $changed"
Write-Host ">> Proyecto listo en: $DestProjectPath"
