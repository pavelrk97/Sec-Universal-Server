[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Output "=== Docker Desktop log files (newest 10) ==="
$logDir = Join-Path $env:LOCALAPPDATA 'Docker\log\host'
if (Test-Path $logDir) {
    Get-ChildItem $logDir | Sort-Object LastWriteTime -Descending |
        Select-Object -First 10 | Format-Table Name, Length, LastWriteTime -AutoSize
} else {
    Write-Output "NO_LOG_DIR $logDir"
}

Write-Output ""
Write-Output "=== Named pipes containing 'docker' ==="
try {
    [System.IO.Directory]::GetFiles('\\.\pipe\') |
        Where-Object { $_ -match 'docker|Docker' } |
        Sort-Object
} catch {
    Write-Output "PIPE_ENUM_ERROR: $_"
}

Write-Output ""
Write-Output "=== Process on port 2375 ==="
$conn = Get-NetTCPConnection -LocalPort 2375 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($conn) {
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
    "PID={0} Name={1} Path={2}" -f $conn.OwningProcess, $proc.ProcessName, $proc.Path
}

Write-Output ""
Write-Output "=== docker info (CLI, current context) ==="
docker info --format '{{.ServerVersion}} | Driver={{.Driver}} | NCPU={{.NCPU}} | OSType={{.OSType}} | KernelVersion={{.KernelVersion}}'

Write-Output ""
Write-Output "=== curl localhost:2375/_ping (no UA, default UA, Java UA) ==="
foreach ($ua in @('', 'Java-http-client/21', 'Apache-HttpClient/5.3 (Java/21)')) {
    $hdr = if ($ua) { @{ 'User-Agent' = $ua } } else { @{} }
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:2375/_ping' -Headers $hdr -UseBasicParsing -ErrorAction Stop
        "UA='$ua' -> HTTP $($r.StatusCode) body='$($r.Content)'"
    } catch {
        "UA='$ua' -> ERROR $_"
    }
}

Write-Output ""
Write-Output "=== JVM-style probe: HEAD vs GET /info via different paths ==="
foreach ($url in @('http://localhost:2375/info','http://localhost:2375/v1.43/info','http://localhost:2375/v1.45/info','http://localhost:2375/version')) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -ErrorAction Stop
        "GET $url -> HTTP $($r.StatusCode)  bodyHead='$($r.Content.Substring(0,[Math]::Min(120,$r.Content.Length)))'"
    } catch {
        "GET $url -> ERROR $_"
    }
}
