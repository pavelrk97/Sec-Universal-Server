[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Output "=== curl direct to named pipes (this is the smoking gun) ==="
foreach ($pipe in @('docker_engine','docker_cli','dockerDesktopLinuxEngine','dockerDesktopEngine')) {
    Write-Output "--- pipe: $pipe ---"
    & curl.exe --silent --max-time 5 --unix-socket "\\.\pipe\$pipe" `
        --write-out "HTTP %{http_code}`n" `
        "http://localhost/_ping"
    & curl.exe --silent --max-time 5 --unix-socket "\\.\pipe\$pipe" `
        --write-out "`nHTTP %{http_code} (info)`n" `
        "http://localhost/info"
    Write-Output ""
}

Write-Output ""
Write-Output "=== GET /info via TCP 2375 with various paths ==="
foreach ($url in @('http://localhost:2375/info','http://localhost:2375/v1.43/info','http://localhost:2375/v1.45/info','http://localhost:2375/version')) {
    & curl.exe --silent --max-time 5 --write-out "`nHTTP %{http_code} | size=%{size_download}`n" $url | Select-Object -First 3
    Write-Output "---"
}
