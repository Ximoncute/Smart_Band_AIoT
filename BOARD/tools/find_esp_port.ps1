$ErrorActionPreference = 'Stop'
Get-CimInstance Win32_PnPEntity |
    Where-Object {
        $_.PNPDeviceID -match 'VID_(303A&PID_1001|10C4&PID_EA60|1A86&PID_7523|0403&PID_6001)' -and
        $_.Name -match '\(COM\d+\)'
    } |
    ForEach-Object { [regex]::Match($_.Name, 'COM\d+').Value }
