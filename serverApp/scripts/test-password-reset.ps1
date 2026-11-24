param(
    [string]$PostgresBin = 'C:/Program Files/PostgreSQL/17/bin',
    [string]$TestFilter,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$resetServerRoot = Split-Path -Parent $PSScriptRoot
$resetData = Join-Path $resetServerRoot ('build/reset-test-' + [guid]::NewGuid().ToString('N'))
$resetLog = $resetData + '.log'
$resetListener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$resetListener.Start()
$resetPort = $resetListener.LocalEndpoint.Port
$resetListener.Stop()
$resetEnvironmentNames = @('EVMAP_TEST_DB_URL', 'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'SPRING_FLYWAY_LOCATIONS', 'SPRING_PROFILES_ACTIVE', 'SPRING_APPLICATION_JSON')
$resetPreviousEnvironment = @{}
foreach ($resetName in $resetEnvironmentNames) { $resetPreviousEnvironment[$resetName] = [Environment]::GetEnvironmentVariable($resetName) }
$resetStarted = $false
Push-Location $resetServerRoot
try {
    & (Join-Path $PostgresBin 'initdb.exe') -D $resetData -U reset_test --auth=trust --encoding=UTF8 --locale=C --no-sync --set shared_buffers=16MB --set max_connections=30
    if ($LASTEXITCODE -ne 0) { throw 'Failed to initialize test PostgreSQL' }
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $resetData -l $resetLog -o "-h 127.0.0.1 -p $resetPort" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Failed to start test PostgreSQL' }
    $resetStarted = $true
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $resetPort -U reset_test evmap_password_reset_test
    if ($LASTEXITCODE -ne 0) { throw 'Failed to create test database' }
    & (Join-Path $PostgresBin 'psql.exe') -h 127.0.0.1 -p $resetPort -U reset_test -d evmap_password_reset_test -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA evmap; CREATE EXTENSION postgis WITH SCHEMA evmap; CREATE EXTENSION pg_trgm WITH SCHEMA evmap;'
    if ($LASTEXITCODE -ne 0) { throw 'PostGIS and pg_trgm must be installed for this PostgreSQL version' }
    $env:EVMAP_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$resetPort/evmap_password_reset_test?currentSchema=evmap,public"
    $env:SPRING_DATASOURCE_URL = $env:EVMAP_TEST_DB_URL
    $env:SPRING_DATASOURCE_USERNAME = 'reset_test'
    $env:SPRING_DATASOURCE_PASSWORD = ''
    $env:SPRING_PROFILES_ACTIVE = 'local,test-legacy'
    $env:SPRING_FLYWAY_LOCATIONS = 'classpath:db/migration'
    # Test-only key. SMTP is mocked in the recovery integration tests.
    $env:SPRING_APPLICATION_JSON = '{"app":{"security":{"password-reset":{"mail-poll-ms":86400000,"cleanup-ms":86400000,"secret":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="}}}}'
    $resetGradleArguments = @('--no-daemon', 'test')
    if ($Offline) { $resetGradleArguments += '--offline' }
    if ($TestFilter) { $resetGradleArguments += @('--tests', $TestFilter) }
    & ./gradlew.bat @resetGradleArguments
    if ($LASTEXITCODE -ne 0) { throw 'Server tests failed; inspect build/reports/tests/test/index.html' }
} finally {
    if ($resetStarted) { & (Join-Path $PostgresBin 'pg_ctl.exe') -D $resetData -m fast -w stop }
    foreach ($resetName in $resetEnvironmentNames) { [Environment]::SetEnvironmentVariable($resetName, $resetPreviousEnvironment[$resetName]) }
    Pop-Location
}
