param(
    [Parameter(Mandatory)][ValidatePattern('^(\d{1,3}\.){3}\d{1,3}$')][string]$IpAddress,
    [string]$SshKey = "$env:USERPROFILE/.ssh/evmap-lightsail-eu-north-1.pem",
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$evmapRepository = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome/bin;$env:PATH"
}
Push-Location (Join-Path $evmapRepository 'serverApp')
try {
    & ./gradlew.bat --no-daemon bootJar
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }
} finally { Pop-Location }
$evmapArtifact = Join-Path $evmapRepository 'serverApp/build/libs/serverApp-0.0.1-SNAPSHOT.jar'
$evmapRemote = "ubuntu@$IpAddress"
# Requires a host key already verified during the first server setup.
& ssh -i $SshKey -o StrictHostKeyChecking=yes $evmapRemote 'sudo systemctl start evmap-backup.service'
if ($LASTEXITCODE -ne 0) { throw 'Backup failed; deployment stopped' }
& scp -i $SshKey $evmapArtifact "${evmapRemote}:/home/ubuntu/backend.jar"
if ($LASTEXITCODE -ne 0) { throw 'JAR upload failed' }
& scp -i $SshKey (Join-Path $PSScriptRoot 'release.sh') "${evmapRemote}:/home/ubuntu/evmap-deploy/release.sh"
if ($LASTEXITCODE -ne 0) { throw 'Release script upload failed' }
& ssh -i $SshKey $evmapRemote 'sudo bash /home/ubuntu/evmap-deploy/release.sh /home/ubuntu/backend.jar'
if ($LASTEXITCODE -ne 0) { throw 'Deployment health check failed' }
$evmapHealth = Invoke-RestMethod "https://$IpAddress/health" -TimeoutSec 20
if ($evmapHealth.status -ne 'UP') { throw 'Public health check failed' }
Write-Output "Backend available: https://$IpAddress/"
