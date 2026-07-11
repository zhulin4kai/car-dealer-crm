$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..')
$EnvFile = Join-Path $RepoRoot '.env.demo'
$ComposeFile = Join-Path $RepoRoot 'compose.yaml'
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

function Test-CommandExists {
    param([string] $Command)
    return $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

function State-HasAction {
    param([string] $Action)
    return (Test-Path $StateActionsFile) -and ((Get-Content $StateActionsFile) -contains $Action)
}

function Get-StateValue {
    param(
        [string] $Key,
        [string] $DefaultValue = ''
    )
    if (Test-Path $StateEnvFile) {
        $line = Get-Content $StateEnvFile | Where-Object { $_ -match "^$Key=" } | Select-Object -Last 1
        if ($line) {
            return ($line -split '=', 2)[1]
        }
    }
    return $DefaultValue
}

function Get-EnvValue {
    param(
        [string] $Key,
        [string] $DefaultValue = ''
    )
    if (Test-Path $EnvFile) {
        $line = Get-Content $EnvFile | Where-Object { $_ -match "^$Key=" } | Select-Object -Last 1
        if ($line) {
            return ($line -split '=', 2)[1]
        }
    }
    return $DefaultValue
}

function Remove-StateAction {
    param([string] $Action)
    if (-not (Test-Path $StateActionsFile)) {
        return
    }
    Get-Content $StateActionsFile |
        Where-Object { $_ -ne $Action } |
        Set-Content -Encoding UTF8 $StateActionsFile
}

function Resolve-ComposeCommand {
    if (Test-CommandExists 'docker') {
        try {
            docker compose version *> $null
            if ($LASTEXITCODE -eq 0) {
                return 'docker compose'
            }
        } catch {
        }
    }
    if (Test-CommandExists 'docker-compose') {
        return 'docker-compose'
    }
    return ''
}

function Add-Candidate {
    param(
        [System.Collections.Generic.List[object]] $Candidates,
        [string] $Key,
        [string] $Label
    )
    $Candidates.Add([pscustomobject]@{ Key = $Key; Label = $Label }) | Out-Null
}

function Collect-Candidates {
    $candidates = [System.Collections.Generic.List[object]]::new()
    $composeCommand = Resolve-ComposeCommand
    if ($composeCommand -and (Test-Path $ComposeFile)) {
        Add-Candidate $candidates 'compose_project' "项目容器（含 AI）、网络、业务数据卷、AI Provider 主密钥卷和本地构建镜像（$composeCommand down --volumes --rmi local）"
    }
    if (State-HasAction 'runtime_images') {
        Add-Candidate $candidates 'runtime_images' '启动脚本拉取过的 MySQL/Redis 镜像'
    }
    if (State-HasAction 'docker_desktop_windows') {
        Add-Candidate $candidates 'docker_desktop_windows' '启动脚本安装的 Docker Desktop for Windows'
    }
    if (Test-Path $StateDir) {
        Add-Candidate $candidates 'install_record' "安装记录目录 $StateDir"
    }
    return $candidates
}

function Choose-Candidates {
    param([System.Collections.Generic.List[object]] $Candidates)

    if ($Candidates.Count -eq 0) {
        Write-Info '未发现可由脚本定位的清理项。'
        exit 0
    }

    Write-Host '即将可删除的项目如下：'
    for ($i = 0; $i -lt $Candidates.Count; $i++) {
        Write-Host "  $($i + 1)) $($Candidates[$i].Label)"
    }
    Write-Host ''
    Write-Host '请选择删除范围：'
    Write-Host '  1) 删除全部'
    Write-Host '  2) 选择部分删除'
    Write-Host '  3) 退出'

    while ($true) {
        $choice = Read-Host '请选择 [3]'
        if ([string]::IsNullOrWhiteSpace($choice)) {
            $choice = '3'
        }
        switch ($choice) {
            '1' { return @($Candidates) }
            '2' { return Choose-PartialCandidates $Candidates }
            '3' { exit 0 }
            default { Write-Host '请输入 1、2 或 3。' }
        }
    }
}

function Choose-PartialCandidates {
    param([System.Collections.Generic.List[object]] $Candidates)

    while ($true) {
        $answer = Read-Host '请输入要删除的编号，用英文逗号分隔，例如 1,3'
        if ([string]::IsNullOrWhiteSpace($answer)) {
            continue
        }

        $selected = [System.Collections.Generic.List[object]]::new()
        $valid = $true
        foreach ($item in $answer.Split(',')) {
            $trimmed = $item.Trim()
            $index = 0
            if (-not [int]::TryParse($trimmed, [ref] $index) -or $index -lt 1 -or $index -gt $Candidates.Count) {
                $valid = $false
                break
            }
            $selected.Add($Candidates[$index - 1]) | Out-Null
        }

        if ($valid -and $selected.Count -gt 0) {
            return @($selected)
        }
        Write-Host '编号无效，请重新输入。'
    }
}

function Confirm-Deletion {
    param([object[]] $Selected)
    Write-Host ''
    Write-Host '将删除以下内容：'
    foreach ($item in $Selected) {
        Write-Host "  - $($item.Label)"
    }
    Write-Host ''
    $answer = Read-Host '这是不可逆操作。请输入 DELETE 确认删除'
    if ($answer -ne 'DELETE') {
        throw '未确认删除，已退出。'
    }
}

function Remove-ComposeProject {
    $composeCommand = Resolve-ComposeCommand
    if (-not $composeCommand) {
        Write-Warn '未检测到 Docker Compose，跳过项目容器清理。'
        return
    }
    if (-not (Test-Path $ComposeFile)) {
        return
    }
    if ($composeCommand -eq 'docker compose') {
        docker compose --env-file $EnvFile -f $ComposeFile down --volumes --remove-orphans --rmi local
    } else {
        docker-compose --env-file $EnvFile -f $ComposeFile down --volumes --remove-orphans --rmi local
    }
    Remove-StateAction 'compose_project'
}

function Remove-RuntimeImages {
    if (-not (Test-CommandExists 'docker')) {
        Write-Warn '未检测到 docker 命令，跳过镜像删除。'
        return
    }
    $mysqlImage = Get-StateValue 'MYSQL_IMAGE' (Get-EnvValue 'MYSQL_IMAGE' '')
    $redisImage = Get-StateValue 'REDIS_IMAGE' (Get-EnvValue 'REDIS_IMAGE' '')
    if ($mysqlImage) {
        docker image rm $mysqlImage
    }
    if ($redisImage) {
        docker image rm $redisImage
    }
    Remove-StateAction 'runtime_images'
}

function Remove-DockerDesktopWindows {
    if (Test-CommandExists 'winget') {
        winget uninstall -e --id Docker.DockerDesktop
    } else {
        $uninstallKeys = @(
            'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
            'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
            'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
        )
        $entry = Get-ItemProperty $uninstallKeys -ErrorAction SilentlyContinue |
            Where-Object { $_.DisplayName -like 'Docker Desktop*' } |
            Select-Object -First 1
        if ($entry -and $entry.UninstallString) {
            Start-Process -FilePath 'cmd.exe' -ArgumentList "/c $($entry.UninstallString)" -Wait
        } else {
            Write-Warn '未找到 Docker Desktop 卸载入口，跳过 Docker Desktop 卸载。'
        }
    }
    Remove-StateAction 'docker_desktop_windows'
}

function Remove-InstallRecord {
    if (Test-Path $StateDir) {
        Remove-Item -Recurse -Force $StateDir
    }
}

function Execute-Cleanup {
    param([object[]] $Selected)
    foreach ($item in $Selected) {
        switch ($item.Key) {
            'compose_project' { Remove-ComposeProject }
            'runtime_images' { Remove-RuntimeImages }
            'docker_desktop_windows' { Remove-DockerDesktopWindows }
            'install_record' { Remove-InstallRecord }
        }
    }
}

$candidates = Collect-Candidates
$selected = Choose-Candidates $candidates
Confirm-Deletion $selected
Execute-Cleanup $selected
Write-Info '清理完成。'
