#!/usr/bin/env fish

set script_path (status --current-filename)
set script_dir (cd (dirname "$script_path"); and pwd)
set kernel (uname -s)

echo "[INFO] 检测到 Shell: fish $FISH_VERSION"

switch "$kernel"
    case Darwin Linux
        if type -q bash
            exec bash "$script_dir/demo-bootstrap.sh" $argv
        else
            echo "[ERROR] 未检测到 bash，无法执行 macOS/Linux 安装入口。" >&2
            exit 1
        end
    case 'MINGW*' 'MSYS*' 'CYGWIN*'
        if type -q powershell.exe
            powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$script_dir/demo-bootstrap.ps1"
        else
            echo "[ERROR] 未检测到 powershell.exe，无法执行 Windows 安装入口。" >&2
            exit 1
        end
    case '*'
        echo "[ERROR] 无法识别系统: $kernel" >&2
        exit 1
end
