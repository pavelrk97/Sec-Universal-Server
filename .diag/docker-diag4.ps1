[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Output "=== TCP 2375 probe variations ==="
foreach ($url in @('http://localhost:2375/_ping','http://localhost:2375/info','http://localhost:2375/v1.43/info','http://localhost:2375/v1.45/info','http://localhost:2375/v1.54/info','http://localhost:2375/version','http://127.0.0.1:2375/info')) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        $body = $r.Content.Substring(0, [Math]::Min(100, $r.Content.Length))
        "OK  $url -> $($r.StatusCode), head=$body"
    } catch {
        "ERR $url -> $_"
    }
}

Write-Output ""
Write-Output "=== httpproxy.log tail (last 80 lines, looking for stub/com.docker.desktop.address) ==="
$logDir = Join-Path $env:LOCALAPPDATA 'Docker\log\host'
Get-Content (Join-Path $logDir 'httpproxy.log') -Tail 80

Write-Output ""
Write-Output "=== com.docker.backend.exe.log tail (last 60 lines, grep label/stub/2375) ==="
Get-Content (Join-Path $logDir 'com.docker.backend.exe.log') -Tail 200 |
    Where-Object { $_ -match '(2375|stub|com\.docker\.desktop\.address|api-proxy|engineProxy)' } |
    Select-Object -First 30
