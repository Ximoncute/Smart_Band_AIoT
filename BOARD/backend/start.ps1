$ErrorActionPreference = 'Stop'
$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mongo = 'C:\Program Files\MongoDB\Server\8.2\bin\mongod.exe'
$dataDir = Join-Path $env:LOCALAPPDATA 'SmartbandMongo'

if (-not (Test-Path -LiteralPath $mongo)) {
    throw 'Chưa cài MongoDB Server 8.2. Hãy cài MongoDB Community Server.'
}
New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
$running = Test-NetConnection 127.0.0.1 -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $running) {
    Start-Process -FilePath $mongo -ArgumentList @('--dbpath', $dataDir, '--bind_ip', '127.0.0.1', '--port', '27017') -WindowStyle Hidden
    Start-Sleep -Seconds 2
}
python -m pip install -r (Join-Path $backendDir 'requirements.txt')
python (Join-Path $backendDir 'server.py')
