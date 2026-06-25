$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..')
$EnvFile = Join-Path $RepoRoot '.env.demo'
$ComposeFile = Join-Path $RepoRoot 'compose.yaml'
$DockerHubMirrorCandidates = @(
    'https://docker.1ms.run',
    'https://docker.m.daocloud.io',
    'https://docker.1panel.live',
    'https://docker.xuanyuan.me'
)
$SelectedDockerHubLibraryPrefix = ''
$RuntimeEnvFile = ''
$WebPortValue = ''
$ServerPortValue = ''
$MysqlPortValue = ''
$RedisPortValue = ''
$StateDir = Join-Path $HOME '.car-dealer-crm-demo'
$StateActionsFile = Join-Path $StateDir 'actions.txt'
$StateEnvFile = Join-Path $StateDir 'state.env'

function Write-Info {
    param([string] $Message)
    Write-Host "[INFO] $Message"
}

function Write-Warn {
    param([string] $Message)
    Write-Warning $Message
}

function Ask-Choice {
    param(
        [string] $Message,
        [string] $DefaultChoice,
        [string[]] $ValidChoices,
        [string[]] $Options
    )

    Write-Host $Message
    foreach ($option in $Options) {
        Write-Host "  $option"
    }

    while ($true) {
        $answer = Read-Host "请选择 [$DefaultChoice]"
        if ([string]::IsNullOrWhiteSpace($answer)) {
            $answer = $DefaultChoice
        }

        if ($ValidChoices -contains $answer) {
            return $answer
        }

        Write-Host "请输入有效选项：$($ValidChoices -join ', ')。"
    }
}

function Test-CommandExists {
    param([string] $Command)
    return $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

function Ensure-StateDir {
    if (-not (Test-Path $StateDir)) {
        New-Item -ItemType Directory -Path $StateDir | Out-Null
    }
}

function Record-StateAction {
    param([string] $Action)
    Ensure-StateDir
    if (-not (Test-Path $StateActionsFile)) {
        New-Item -ItemType File -Path $StateActionsFile | Out-Null
    }
    $actions = Get-Content $StateActionsFile
    if ($actions -notcontains $Action) {
        Add-Content -Encoding UTF8 $StateActionsFile $Action
    }
}

function Set-StateValue {
    param(
        [string] $Key,
        [string] $Value
    )
    Ensure-StateDir
    $lines = @()
    if (Test-Path $StateEnvFile) {
        $lines = Get-Content $StateEnvFile | Where-Object { $_ -notmatch "^$Key=" }
    }
    $lines += "$Key=$Value"
    Set-Content -Encoding UTF8 $StateEnvFile $lines
}

function Record-CommonState {
    Set-StateValue 'PROJECT_ROOT' "$RepoRoot"
    Set-StateValue 'COMPOSE_FILE' "$ComposeFile"
    Set-StateValue 'ENV_FILE' "$EnvFile"
    Set-StateValue 'UPDATED_AT' ([DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'))
}

function ConvertTo-LibraryPrefix {
    param([string] $Mirror)
    $value = $Mirror.TrimEnd('/')
    $value = $value -replace '^https?://', ''
    return "$value/library/"
}

function Test-RegistryMirror {
    param([string] $Mirror)

    $uri = "$($Mirror.TrimEnd('/'))/v2/"
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -Method Get -TimeoutSec 8
        return ($response.StatusCode -eq 200 -or $response.StatusCode -eq 401)
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $statusCode = [int] $response.StatusCode
            return ($statusCode -eq 200 -or $statusCode -eq 401)
        }
        return $false
    }
}

function Get-BaseEnvValue {
    param([string] $Name)
    if (-not (Test-Path $EnvFile)) {
        return ''
    }

    $line = Get-Content $EnvFile | Where-Object { $_ -match "^$Name=" } | Select-Object -Last 1
    if ($line) {
        return ($line -split '=', 2)[1]
    }
    return ''
}

function Test-PortInUse {
    param([int] $Port)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $asyncResult = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(500)
        if ($connected) {
            $client.EndConnect($asyncResult)
            return $true
        }
        return $false
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Find-AvailablePort {
    param([int] $StartPort)

    $port = $StartPort
    while (Test-PortInUse $port) {
        $port++
    }
    return $port
}

function Read-PortValue {
    param(
        [string] $Message,
        [int] $DefaultPort
    )

    while ($true) {
        $answer = Read-Host "$Message [$DefaultPort]"
        if ([string]::IsNullOrWhiteSpace($answer)) {
            $answer = "$DefaultPort"
        }

        $port = 0
        if ([int]::TryParse($answer, [ref] $port) -and $port -ge 1 -and $port -le 65535) {
            return $port
        }

        Write-Host '请输入 1-65535 之间的端口号。'
    }
}

function Resolve-HostPort {
    param(
        [string] $Key,
        [string] $Label,
        [int] $DefaultPort
    )

    $baseValue = Get-BaseEnvValue $Key
    $port = $DefaultPort
    if (-not [string]::IsNullOrWhiteSpace($baseValue)) {
        $port = [int] $baseValue
    }

    if (-not (Test-PortInUse $port)) {
        return $port
    }

    $suggested = Find-AvailablePort ($port + 1)
    $choice = Ask-Choice `
        "$Label 端口 $port 已被占用，请选择处理方式：" `
        '1' `
        @('1', '2', '3', '4') `
        @(
            "1) 使用脚本建议的可用端口：$suggested",
            '2) 手动输入其他端口',
            "3) 继续使用 $port，我自己处理冲突",
            '4) 退出脚本'
        )

    switch ($choice) {
        '1' { return $suggested }
        '2' { return Read-PortValue "请输入 $Label 端口" $suggested }
        '3' { return $port }
        '4' { throw '已退出。请处理端口冲突后重新运行脚本。' }
    }
}

function Resolve-HostPorts {
    $script:WebPortValue = Resolve-HostPort 'WEB_PORT' '前端 Web' 8080
    $script:ServerPortValue = Resolve-HostPort 'SERVER_PORT' '后端 API' 8089
    $script:MysqlPortValue = Resolve-HostPort 'MYSQL_PORT' 'MySQL' 13306
    $script:RedisPortValue = Resolve-HostPort 'REDIS_PORT' 'Redis' 16379
}

function Select-DockerHubMirror {
    $mirrorLine = Get-BaseEnvValue 'DOCKERHUB_MIRRORS'
    if (-not [string]::IsNullOrWhiteSpace($mirrorLine)) {
        $script:DockerHubMirrorCandidates = $mirrorLine.Split(',') |
            ForEach-Object { $_.Trim() } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    }

    foreach ($mirror in $DockerHubMirrorCandidates) {
        Write-Info "测试 Docker Hub 镜像连通性: $mirror"
        if (Test-RegistryMirror $mirror) {
            $script:SelectedDockerHubLibraryPrefix = ConvertTo-LibraryPrefix $mirror
            Write-Info "使用 Docker Hub 镜像: $mirror"
            return
        }
    }

    $script:SelectedDockerHubLibraryPrefix = ''
    Write-Warn '大陆 Docker Hub 镜像均未通过连通性检查，将回退到官方镜像源。'
}

function Refresh-DockerPath {
    $paths = @(
        (Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Docker\Docker\resources\bin')
    )

    foreach ($path in $paths) {
        if ((Test-Path $path) -and (-not ($env:PATH -split ';' | Where-Object { $_ -eq $path }))) {
            $env:PATH = "$path;$env:PATH"
        }
    }
}

function Test-DockerReady {
    Refresh-DockerPath
    if (-not (Test-CommandExists 'docker')) {
        return $false
    }

    try {
        docker info *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Wait-DockerReady {
    Write-Info '检查 Docker Engine 是否可用...'
    for ($i = 0; $i -lt 80; $i++) {
        if (Test-DockerReady) {
            return $true
        }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Start-DockerDesktop {
    $paths = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Docker\Docker\Docker Desktop.exe'),
        (Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe')
    )

    foreach ($path in $paths) {
        if (Test-Path $path) {
            Write-Info "启动 Docker Desktop: $path"
            Start-Process $path
            return
        }
    }

    Write-Warn '未找到 Docker Desktop 启动程序，请从开始菜单手动启动 Docker Desktop。'
}

function Invoke-DockerDesktopDirectInstaller {
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()
    if ($arch -eq 'arm64') {
        $url = 'https://desktop.docker.com/win/main/arm64/Docker%20Desktop%20Installer.exe'
    } else {
        $url = 'https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe'
    }

    $installer = Join-Path $env:TEMP 'Docker Desktop Installer.exe'
    Write-Info "下载 Docker Desktop: $url"
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $installer
    Write-Info '执行 Docker Desktop 当前用户安装。'
    Start-Process $installer -Wait -ArgumentList @('install', '--user', '--accept-license')
}

function Install-DockerDesktop {
    Write-Info '准备在 Windows 上安装 Docker Desktop。'

    if (Test-CommandExists 'winget') {
        $choice = Ask-Choice `
            '检测到 winget，请选择 Docker Desktop 安装方式：' `
            '1' `
            @('1', '2', '3') `
            @(
                '1) 使用 winget 安装 Docker Desktop',
                '2) 下载官方安装器并安装',
                '3) 退出脚本，我自己安装'
            )

        switch ($choice) {
            '1' {
                winget install -e --id Docker.DockerDesktop --accept-source-agreements --accept-package-agreements
                Record-CommonState
                Set-StateValue 'DOCKER_DESKTOP_INSTALL_METHOD' 'winget'
                Record-StateAction 'docker_desktop_windows'
                return
            }
            '2' {
                Invoke-DockerDesktopDirectInstaller
                Record-CommonState
                Set-StateValue 'DOCKER_DESKTOP_INSTALL_METHOD' 'installer'
                Record-StateAction 'docker_desktop_windows'
                return
            }
            '3' {
                throw '已退出。请自行安装并启动 Docker 后重新运行脚本。'
            }
        }
    }

    $choice = Ask-Choice `
        '未检测到 winget，请选择 Docker Desktop 安装方式：' `
        '1' `
        @('1', '2') `
        @(
            '1) 下载官方安装器并安装',
            '2) 退出脚本，我自己安装'
        )

    switch ($choice) {
        '1' {
            Invoke-DockerDesktopDirectInstaller
            Record-CommonState
            Set-StateValue 'DOCKER_DESKTOP_INSTALL_METHOD' 'installer'
            Record-StateAction 'docker_desktop_windows'
        }
        '2' { throw '已退出。请自行安装并启动 Docker 后重新运行脚本。' }
    }
}

function Ensure-Docker {
    Refresh-DockerPath

    while ($true) {
        Refresh-DockerPath
        if ((Test-CommandExists 'docker') -and (Wait-DockerReady)) {
            Write-Info 'Docker 已可用。'
            return
        }

        if (Test-CommandExists 'docker') {
            Write-Warn '检测到 docker 命令，但 Docker Engine 当前不可用。'
            $choice = Ask-Choice `
                '请选择 Docker Engine 处理方式：' `
                '1' `
                @('1', '2', '3') `
                @(
                    '1) 脚本尝试启动 Docker Desktop 并重新检测',
                    '2) 我已自己处理好，现在重新检测',
                    '3) 退出脚本，我自己处理'
                )

            switch ($choice) {
                '1' {
                    Start-DockerDesktop
                    Start-Sleep -Seconds 5
                }
                '2' {}
                '3' { throw '已退出。请自行处理 Docker 后重新运行脚本。' }
            }
            continue
        }

        $choice = Ask-Choice `
            '未检测到 Docker，请选择处理方式：' `
            '1' `
            @('1', '2', '3') `
            @(
                '1) 脚本自动安装 Docker Desktop',
                '2) 我已自己安装好，现在重新检测',
                '3) 退出脚本，我自己安装'
            )

        switch ($choice) {
            '1' {
                Install-DockerDesktop
                Refresh-DockerPath
                Start-DockerDesktop
                Start-Sleep -Seconds 5
            }
            '2' {}
            '3' { throw '已退出。请自行安装并启动 Docker 后重新运行脚本。' }
        }
    }
}

function Print-Environment {
    Write-Info "检测到系统: Windows"
    Write-Info "检测到 Shell: PowerShell $($PSVersionTable.PSVersion)"
    Write-Info "项目目录: $RepoRoot"
}

function Print-DockerStatus {
    Write-Info "Docker 版本: $(docker --version)"
    Write-Info "Compose 版本: $(docker compose version)"
}

function Test-ComposeReady {
    try {
        docker compose version *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Ensure-Compose {
    while ($true) {
        if (Test-ComposeReady) {
            return
        }

        $choice = Ask-Choice `
            '未检测到 Docker Compose，请选择处理方式：' `
            '1' `
            @('1', '2', '3') `
            @(
                '1) 脚本尝试启动或修复 Docker Desktop 后重新检测',
                '2) 我已自己处理好，现在重新检测',
                '3) 退出脚本，我自己处理'
            )

        switch ($choice) {
            '1' {
                Start-DockerDesktop
                Start-Sleep -Seconds 5
            }
            '2' {}
            '3' { throw '已退出。请自行安装 Docker Compose 后重新运行脚本。' }
        }
    }
}

function Get-EnvValue {
    param([string] $Name)
    $file = if ($RuntimeEnvFile) { $RuntimeEnvFile } else { $EnvFile }
    $line = Get-Content $file | Where-Object { $_ -match "^$Name=" } | Select-Object -Last 1
    if ($line) {
        return ($line -split '=', 2)[1]
    }
    return ''
}

function New-RuntimeEnvFile {
    if (-not (Test-Path $EnvFile)) {
        throw "缺少 $EnvFile"
    }

    Remove-RuntimeEnvFile
    $script:RuntimeEnvFile = Join-Path $env:TEMP "car-dealer-crm-$PID.env"
    Get-Content $EnvFile |
        Where-Object { $_ -notmatch '^(DOCKERHUB_LIBRARY_PREFIX|MYSQL_IMAGE|REDIS_IMAGE|WEB_PORT|SERVER_PORT|MYSQL_PORT|REDIS_PORT)=' } |
        Set-Content -Encoding UTF8 $RuntimeEnvFile

    $mysqlImage = 'mysql:8.0'
    $redisImage = 'redis:7.4-alpine'
    if (-not [string]::IsNullOrWhiteSpace($SelectedDockerHubLibraryPrefix)) {
        $mysqlImage = "${SelectedDockerHubLibraryPrefix}mysql:8.0"
        $redisImage = "${SelectedDockerHubLibraryPrefix}redis:7.4-alpine"
    }

    Add-Content -Encoding UTF8 $RuntimeEnvFile "DOCKERHUB_LIBRARY_PREFIX=$SelectedDockerHubLibraryPrefix"
    Add-Content -Encoding UTF8 $RuntimeEnvFile "MYSQL_IMAGE=$mysqlImage"
    Add-Content -Encoding UTF8 $RuntimeEnvFile "REDIS_IMAGE=$redisImage"
    Add-Content -Encoding UTF8 $RuntimeEnvFile "WEB_PORT=$WebPortValue"
    Add-Content -Encoding UTF8 $RuntimeEnvFile "SERVER_PORT=$ServerPortValue"
    Add-Content -Encoding UTF8 $RuntimeEnvFile "MYSQL_PORT=$MysqlPortValue"
    Add-Content -Encoding UTF8 $RuntimeEnvFile "REDIS_PORT=$RedisPortValue"
}

function Remove-RuntimeEnvFile {
    if ($RuntimeEnvFile -and (Test-Path $RuntimeEnvFile)) {
        Remove-Item -Force $RuntimeEnvFile
    }
}

function Pull-RuntimeImages {
    Write-Info '拉取 MySQL 和 Redis 镜像。'
    docker compose --env-file $RuntimeEnvFile -f $ComposeFile pull mysql redis
    if ($LASTEXITCODE -eq 0) {
        Record-CommonState
        Set-StateValue 'MYSQL_IMAGE' (Get-EnvValue 'MYSQL_IMAGE')
        Set-StateValue 'REDIS_IMAGE' (Get-EnvValue 'REDIS_IMAGE')
        Record-StateAction 'runtime_images'
        return
    }

    Write-Warn '当前镜像源拉取 MySQL/Redis 失败，尝试其他镜像源。'

    foreach ($mirror in $DockerHubMirrorCandidates) {
        $mirror = $mirror.Trim()
        if ([string]::IsNullOrWhiteSpace($mirror)) {
            continue
        }

        $prefix = ConvertTo-LibraryPrefix $mirror
        if ($prefix -eq $SelectedDockerHubLibraryPrefix) {
            continue
        }

        $script:SelectedDockerHubLibraryPrefix = $prefix
        New-RuntimeEnvFile
        Write-Info "改用 Docker Hub 镜像重新拉取: $mirror"
        docker compose --env-file $RuntimeEnvFile -f $ComposeFile pull mysql redis
        if ($LASTEXITCODE -eq 0) {
            Record-CommonState
            Set-StateValue 'MYSQL_IMAGE' (Get-EnvValue 'MYSQL_IMAGE')
            Set-StateValue 'REDIS_IMAGE' (Get-EnvValue 'REDIS_IMAGE')
            Record-StateAction 'runtime_images'
            return
        }
    }

    $script:SelectedDockerHubLibraryPrefix = ''
    Resolve-HostPorts
    New-RuntimeEnvFile
    Write-Warn '大陆镜像拉取均失败，最后尝试官方 Docker Hub。'
    docker compose --env-file $RuntimeEnvFile -f $ComposeFile pull mysql redis
    if ($LASTEXITCODE -ne 0) {
        throw 'MySQL/Redis 镜像拉取失败。'
    }
    Record-CommonState
    Set-StateValue 'MYSQL_IMAGE' (Get-EnvValue 'MYSQL_IMAGE')
    Set-StateValue 'REDIS_IMAGE' (Get-EnvValue 'REDIS_IMAGE')
    Record-StateAction 'runtime_images'
}

function Run-Project {
    if (-not (Test-Path $ComposeFile)) {
        throw "缺少 $ComposeFile"
    }

    New-RuntimeEnvFile
    Push-Location $RepoRoot
    try {
        Pull-RuntimeImages
        Write-Info '开始构建并启动项目环境。首次执行会下载基础镜像和依赖，耗时较长。'
        docker compose --env-file $RuntimeEnvFile -f $ComposeFile up -d --build
        Record-CommonState
        Record-StateAction 'compose_project'
        docker compose --env-file $RuntimeEnvFile -f $ComposeFile ps
    } finally {
        Pop-Location
        Remove-RuntimeEnvFile
    }

    Write-Info "前端访问地址: http://localhost:$(Get-EnvValue 'WEB_PORT')"
    Write-Info "后端 API 地址: http://localhost:$(Get-EnvValue 'SERVER_PORT')"
    Write-Info "MySQL 本机端口: $(Get-EnvValue 'MYSQL_PORT')，数据库: car_dealer_crm"
}

if (-not $IsWindows -and $PSVersionTable.PSEdition -eq 'Core') {
    throw '当前不是 Windows。macOS/Linux 请运行 scripts/demo-bootstrap.sh。'
}

Print-Environment
Refresh-DockerPath
Ensure-Docker
Ensure-Compose
Print-DockerStatus
Select-DockerHubMirror
Write-Info '项目镜像会使用脚本本次检测到的镜像源；如所有大陆镜像不可达，则回退官方 Docker Hub。'

$choice = Ask-Choice `
    'Docker 环境已就绪，请选择下一步：' `
    '1' `
    @('1', '2') `
    @(
        '1) 现在拉取镜像、构建并启动项目',
        '2) 不启动，只输出后续命令'
    )

if ($choice -eq '1') {
    Run-Project
} else {
    Write-Info '之后可运行：docker compose --env-file .env.demo -f compose.yaml up -d --build'
}
