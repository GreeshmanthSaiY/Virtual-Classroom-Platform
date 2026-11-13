$server = Start-Process node -ArgumentList "scripts\static_server.js" -PassThru
Start-Sleep -Seconds 2

$chromePath = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$screenshotDir = "C:\Users\GS_Y\Downloads\New folder\Mine\Projects\Github\virtual-classroom\docs\screenshots"
$tempDir = "C:\Users\GS_Y\AppData\Local\Temp\chrome-qa"

if (!(Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null
}

$viewports = @(
    @{ w = 320; h = 640; name = "320x640_mobile_compact" },
    @{ w = 375; h = 812; name = "375x812_iphone_standard" },
    @{ w = 390; h = 844; name = "390x844_iphone_modern" },
    @{ w = 430; h = 932; name = "430x932_mobile_large" },
    @{ w = 768; h = 1024; name = "768x1024_tablet_portrait" },
    @{ w = 1024; h = 768; name = "1024x768_tablet_landscape" },
    @{ w = 1280; h = 800; name = "1280x800_laptop" },
    @{ w = 1440; h = 900; name = "1440x900_desktop" },
    @{ w = 1920; h = 1080; name = "1920x1080_full_hd" }
)

$routes = @(
    @{ path = "/"; name = "landing" },
    @{ path = "/login"; name = "login" }
)

try {
    foreach ($vp in $viewports) {
        foreach ($r in $routes) {
            $filename = "vp_$($vp.name)_$($r.name).png"
            $outputPath = Join-Path $screenshotDir $filename
            $url = "http://127.0.0.1:4173$($r.path)"

            Write-Host "Capturing $filename ($($vp.w)x$($vp.h))..."
            
            $args = @(
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--hide-scrollbars",
                "--virtual-time-budget=2000",
                "--user-data-dir=$tempDir",
                "--window-size=$($vp.w),$($vp.h)",
                "--screenshot=""$outputPath""",
                $url
            )

            Start-Process -FilePath $chromePath -ArgumentList $args -Wait -NoNewWindow
            
            if (Test-Path $outputPath) {
                $size = (Get-Item $outputPath).Length
                Write-Host "  ✓ Saved $filename ($size bytes)"
            } else {
                Write-Host "  ✗ Failed $filename"
            }
        }
    }
} finally {
    Write-Host "Stopping static server (PID: $($server.Id))..."
    Stop-Process -Id $server.Id -Force
}
