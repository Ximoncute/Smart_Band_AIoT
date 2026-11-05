$ErrorActionPreference = 'Stop'
$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$dataDir = Join-Path $env:LOCALAPPDATA 'SmartbandMongo'

New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
$running = Test-NetConnection 127.0.0.1 -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $running) {
    $mongoRoot = Join-Path $env:ProgramFiles 'MongoDB\Server'
    $mongo = Get-ChildItem -LiteralPath $mongoRoot -Directory -ErrorAction SilentlyContinue |
        Sort-Object { try { [version]$_.Name } catch { [version]'0.0' } } -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin\mongod.exe' } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if (-not $mongo) {
        throw 'MongoDB Community Server was not found. Run setup-and-run.bat first.'
    }
    Start-Process -FilePath $mongo -ArgumentList @('--dbpath', $dataDir, '--bind_ip', '127.0.0.1', '--port', '27017') -WindowStyle Hidden
    Start-Sleep -Seconds 2
    if (-not (Test-NetConnection 127.0.0.1 -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue)) {
        throw 'MongoDB did not start on port 27017.'
    }
}
py -3.10 -m pip install -r (Join-Path $backendDir 'requirements.txt')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
py -3.10 (Join-Path $backendDir 'server.py')
exit $LASTEXITCODE
