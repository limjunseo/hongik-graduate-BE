param(
  [Parameter(Mandatory = $true)][string]$PublicIp,
  [Parameter(Mandatory = $true)][string]$KeyPath
)
$ErrorActionPreference = "Stop"
if (!(Test-Path -LiteralPath $KeyPath)) { throw "PEM file not found: $KeyPath" }

$repo = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$be = Join-Path $repo "be"
Push-Location $be
try {
  & .\mvnw.cmd -DskipTests clean package
  if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
  $jar = Join-Path $be "target\graduate-be-0.0.1-SNAPSHOT.jar"
  if (!(Test-Path $jar)) { throw "Expected JAR not found: $jar" }
  $target = "ubuntu@" + $PublicIp + ":/home/ubuntu/app.jar"
  & scp -o StrictHostKeyChecking=accept-new -i $KeyPath $jar $target
  if ($LASTEXITCODE -ne 0) { throw "SCP failed" }
  $remote = "sudo mv /home/ubuntu/app.jar /opt/pofit-qa/app.jar && sudo chown ubuntu:ubuntu /opt/pofit-qa/app.jar && sudo systemctl restart pofit-qa && sudo systemctl --no-pager --full status pofit-qa"
  & ssh -o StrictHostKeyChecking=accept-new -i $KeyPath ("ubuntu@" + $PublicIp) $remote
  if ($LASTEXITCODE -ne 0) { throw "Deployment or service restart failed" }
  Write-Host "JAR deployed. Backend HTTPS host is written in /opt/pofit-qa/public-host.txt"
} finally {
  Pop-Location
}
