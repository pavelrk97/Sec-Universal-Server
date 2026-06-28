[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Probe-Pipe {
    param([string]$PipeName)
    Write-Output ""
    Write-Output "--- pipe: $PipeName ---"
    try {
        $client = New-Object System.IO.Pipes.NamedPipeClientStream('.', $PipeName, [System.IO.Pipes.PipeDirection]::InOut)
        $client.Connect(3000)
        $req = "GET /info HTTP/1.1`r`nHost: docker`r`nUser-Agent: probe`r`nAccept: */*`r`nConnection: close`r`n`r`n"
        $bytes = [System.Text.Encoding]::ASCII.GetBytes($req)
        $client.Write($bytes, 0, $bytes.Length)
        $client.Flush()
        $reader = New-Object System.IO.StreamReader($client)
        $resp = $reader.ReadToEnd()
        $client.Dispose()
        # First 600 chars only
        $cut = $resp.Substring(0, [Math]::Min(800, $resp.Length))
        Write-Output $cut
    } catch {
        Write-Output "ERR: $_"
    }
}

Write-Output "=== Probe Docker named pipes (raw HTTP GET /info) ==="
foreach ($p in @('docker_engine','docker_cli','dockerDesktopLinuxEngine','dockerDesktopEngine','dockerDesktopWindowsEngine')) {
    Probe-Pipe -PipeName $p
}

Write-Output ""
Write-Output "=== docker CLI hitting each pipe with -H ==="
foreach ($p in @('docker_engine','dockerDesktopLinuxEngine')) {
    Write-Output "--- docker -H npipe:////./pipe/$p info ---"
    & docker -H "npipe:////./pipe/$p" info --format '{{.ServerVersion}} | Driver={{.Driver}} | NCPU={{.NCPU}}' 2>&1 | Select-Object -First 5
}

Write-Output ""
Write-Output "=== TCP 2375 probe variations ==="
foreach ($url in @('http://localhost:2375/info','http://localhost:2375/v1.43/info','http://localhost:2375/version','http://127.0.0.1:2375/info')) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        $body = $r.Content.Substring(0, [Math]::Min(150, $r.Content.Length))
        "$url -> HTTP $($r.StatusCode), bodyHead=$body"
    } catch {
        "$url -> ERROR $_"
    }
}
