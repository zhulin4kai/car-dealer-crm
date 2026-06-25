#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env.demo"
COMPOSE_FILE="${REPO_ROOT}/compose.yaml"

DOCKER_CE_MIRROR_URL="https://mirrors.tuna.tsinghua.edu.cn/docker-ce"
DOCKERHUB_MIRROR_CANDIDATES=(
  "https://docker.1ms.run"
  "https://docker.m.daocloud.io"
  "https://docker.1panel.live"
  "https://docker.xuanyuan.me"
)
SELECTED_DOCKERHUB_LIBRARY_PREFIX=""
RUNTIME_ENV_FILE=""
WEB_PORT_VALUE=""
SERVER_PORT_VALUE=""
MYSQL_PORT_VALUE=""
REDIS_PORT_VALUE=""
STATE_DIR="${HOME}/.car-dealer-crm-demo"
STATE_ACTIONS_FILE="${STATE_DIR}/actions.txt"
STATE_ENV_FILE="${STATE_DIR}/state.env"

log() {
  printf '[INFO] %s\n' "$*"
}

warn() {
  printf '[WARN] %s\n' "$*" >&2
}

die() {
  printf '[ERROR] %s\n' "$*" >&2
  exit 1
}

ask_choice() {
  local prompt="$1"
  local default_choice="$2"
  local valid_choices="$3"
  shift 3

  if [ ! -t 0 ]; then
    printf '%s\n' "${default_choice}"
    return
  fi

  printf '%s\n' "${prompt}" >&2
  local option
  for option in "$@"; do
    printf '  %s\n' "${option}" >&2
  done

  local answer
  while true; do
    read -r -p "请选择 [${default_choice}]: " answer
    answer="${answer:-${default_choice}}"
    case " ${valid_choices} " in
      *" ${answer} "*)
        printf '%s\n' "${answer}"
        return
        ;;
      *)
        printf '请输入有效选项：%s。\n' "${valid_choices}" >&2
        ;;
    esac
  done
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

ensure_state_dir() {
  mkdir -p "${STATE_DIR}"
}

record_state_action() {
  local action="$1"
  ensure_state_dir
  touch "${STATE_ACTIONS_FILE}"
  if ! grep -qxF "${action}" "${STATE_ACTIONS_FILE}"; then
    printf '%s\n' "${action}" >> "${STATE_ACTIONS_FILE}"
  fi
}

set_state_value() {
  local key="$1"
  local value="$2"
  ensure_state_dir
  local tmp
  tmp="$(mktemp)"
  if [ -f "${STATE_ENV_FILE}" ]; then
    grep -v "^${key}=" "${STATE_ENV_FILE}" > "${tmp}" || true
  fi
  printf '%s=%s\n' "${key}" "${value}" >> "${tmp}"
  mv "${tmp}" "${STATE_ENV_FILE}"
}

record_common_state() {
  set_state_value PROJECT_ROOT "${REPO_ROOT}"
  set_state_value COMPOSE_FILE "${COMPOSE_FILE}"
  set_state_value ENV_FILE "${ENV_FILE}"
  set_state_value UPDATED_AT "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}

get_base_env_value() {
  local key="$1"
  local default_value="${2:-}"

  if [ -f "${ENV_FILE}" ]; then
    local value
    value="$(grep "^${key}=" "${ENV_FILE}" | tail -n 1 | cut -d= -f2- || true)"
    if [ -n "${value}" ]; then
      printf '%s\n' "${value}"
      return
    fi
  fi

  printf '%s\n' "${default_value}"
}

install_packages() {
  if command_exists apt-get; then
    run_sudo apt-get update
    run_sudo apt-get install -y "$@"
  elif command_exists dnf; then
    run_sudo dnf install -y "$@"
  elif command_exists yum; then
    run_sudo yum install -y "$@"
  elif command_exists pacman; then
    run_sudo pacman -Sy --noconfirm "$@"
  elif command_exists zypper; then
    run_sudo zypper --non-interactive install "$@"
  else
    die "无法识别包管理器，不能自动安装缺失工具: $*"
  fi
}

port_in_use() {
  local port="$1"

  if command_exists nc; then
    nc -z 127.0.0.1 "${port}" >/dev/null 2>&1 && return 0
  fi

  if command_exists lsof; then
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1 && return 0
  fi

  if command_exists ss; then
    ss -ltn | awk '{print $4}' | grep -Eq "(:|\\])${port}$" && return 0
  fi

  return 1
}

find_available_port() {
  local port="$1"
  while port_in_use "${port}"; do
    port=$((port + 1))
  done
  printf '%s\n' "${port}"
}

read_port_value() {
  local prompt="$1"
  local default_port="$2"
  local answer

  while true; do
    read -r -p "${prompt} [${default_port}]: " answer
    answer="${answer:-${default_port}}"
    case "${answer}" in
      ''|*[!0-9]*)
        printf '请输入 1-65535 之间的端口号。\n' >&2
        ;;
      *)
        if [ "${answer}" -ge 1 ] && [ "${answer}" -le 65535 ]; then
          printf '%s\n' "${answer}"
          return
        fi
        printf '请输入 1-65535 之间的端口号。\n' >&2
        ;;
    esac
  done
}

resolve_host_port() {
  local key="$1"
  local label="$2"
  local default_port="$3"
  local port
  port="$(get_base_env_value "${key}" "${default_port}")"

  if ! port_in_use "${port}"; then
    printf '%s\n' "${port}"
    return
  fi

  local suggested choice
  suggested="$(find_available_port "$((port + 1))")"
  choice="$(ask_choice "${label} 端口 ${port} 已被占用，请选择处理方式：" "1" "1 2 3 4" \
    "1) 使用脚本建议的可用端口：${suggested}" \
    "2) 手动输入其他端口" \
    "3) 继续使用 ${port}，我自己处理冲突" \
    "4) 退出脚本")"

  case "${choice}" in
    1)
      printf '%s\n' "${suggested}"
      ;;
    2)
      read_port_value "请输入 ${label} 端口" "${suggested}"
      ;;
    3)
      printf '%s\n' "${port}"
      ;;
    4)
      die "已退出。请处理端口冲突后重新运行脚本。"
      ;;
  esac
}

resolve_host_ports() {
  WEB_PORT_VALUE="$(resolve_host_port WEB_PORT "前端 Web" 8080)"
  SERVER_PORT_VALUE="$(resolve_host_port SERVER_PORT "后端 API" 8089)"
  MYSQL_PORT_VALUE="$(resolve_host_port MYSQL_PORT "MySQL" 13306)"
  REDIS_PORT_VALUE="$(resolve_host_port REDIS_PORT "Redis" 16379)"
}

ensure_curl() {
  if command_exists curl; then
    return 0
  fi

  log "未检测到 curl，尝试自动安装。"
  install_packages curl ca-certificates
}

refresh_docker_path() {
  local docker_paths=(
    "/Applications/Docker.app/Contents/Resources/bin"
    "${HOME}/.docker/bin"
  )

  local path_entry
  for path_entry in "${docker_paths[@]}"; do
    if [ -d "${path_entry}" ] && [[ ":${PATH}:" != *":${path_entry}:"* ]]; then
      export PATH="${path_entry}:${PATH}"
    fi
  done
}

run_sudo() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
}

json_array_from_lines() {
  local first=1
  printf '['
  local item
  for item in "$@"; do
    if [ "${first}" -eq 0 ]; then
      printf ','
    fi
    first=0
    printf '"%s"' "${item}"
  done
  printf ']'
}

mirror_to_library_prefix() {
  local mirror="$1"
  mirror="${mirror#https://}"
  mirror="${mirror#http://}"
  mirror="${mirror%/}"
  printf '%s/library/' "${mirror}"
}

test_registry_mirror() {
  local mirror="$1"
  local status
  status="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 5 --max-time 8 "${mirror%/}/v2/" || true)"
  case "${status}" in
    200|401) return 0 ;;
    *) return 1 ;;
  esac
}

select_dockerhub_mirror() {
  if [ -f "${ENV_FILE}" ]; then
    local mirror_line
    mirror_line="$(grep '^DOCKERHUB_MIRRORS=' "${ENV_FILE}" | tail -n 1 | cut -d= -f2- || true)"
    if [ -n "${mirror_line}" ]; then
      IFS=',' read -r -a DOCKERHUB_MIRROR_CANDIDATES <<< "${mirror_line}"
    fi
  fi

  ensure_curl

  local mirror
  for mirror in "${DOCKERHUB_MIRROR_CANDIDATES[@]}"; do
    mirror="${mirror#"${mirror%%[![:space:]]*}"}"
    mirror="${mirror%"${mirror##*[![:space:]]}"}"
    [ -n "${mirror}" ] || continue
    log "测试 Docker Hub 镜像连通性: ${mirror}"
    if test_registry_mirror "${mirror}"; then
      SELECTED_DOCKERHUB_LIBRARY_PREFIX="$(mirror_to_library_prefix "${mirror}")"
      log "使用 Docker Hub 镜像: ${mirror}"
      return 0
    fi
  done

  SELECTED_DOCKERHUB_LIBRARY_PREFIX=""
  warn "大陆 Docker Hub 镜像均未通过连通性检查，将回退到官方镜像源。"
}

detect_os() {
  local kernel
  kernel="$(uname -s)"
  case "${kernel}" in
    Darwin) printf 'macos' ;;
    Linux)
      if [ -r /proc/version ] && grep -qiE 'microsoft|wsl' /proc/version; then
        printf 'wsl'
      else
        printf 'linux'
      fi
      ;;
    MINGW*|MSYS*|CYGWIN*) printf 'windows-bash' ;;
    *) printf 'unknown' ;;
  esac
}

print_environment() {
  local os_name="$1"
  log "检测到系统: ${os_name}"
  log "检测到 Shell: ${SHELL:-unknown}"
  log "项目目录: ${REPO_ROOT}"
}

wait_for_docker() {
  log "检查 Docker Engine 是否可用..."
  local sudo_checked=0
  for _ in $(seq 1 80); do
    if docker info >/dev/null 2>&1; then
      return 0
    fi
    if command_exists sudo; then
      if sudo -n docker info >/dev/null 2>&1; then
        return 0
      fi
      if [ "${sudo_checked}" -eq 0 ] && [ -t 0 ]; then
        sudo_checked=1
        if sudo -v && sudo docker info >/dev/null 2>&1; then
          return 0
        fi
      fi
    fi
    sleep 3
  done
  return 1
}

install_docker_linux() {
  log "准备在 Linux 上安装 Docker Engine。"
  ensure_curl
  record_common_state

  local installer
  installer="$(mktemp)"
  log "下载安装脚本，并使用清华 Docker CE 软件仓库镜像: ${DOCKER_CE_MIRROR_URL}"
  curl -fsSL https://get.docker.com -o "${installer}"
  run_sudo env DOWNLOAD_URL="${DOCKER_CE_MIRROR_URL}" sh "${installer}"
  rm -f "${installer}"

  if command_exists systemctl; then
    run_sudo systemctl enable --now docker || true
  else
    run_sudo service docker start || true
  fi

  if [ "$(id -u)" -ne 0 ]; then
    run_sudo usermod -aG docker "$USER" || true
    warn "已尝试把当前用户加入 docker 组；如果本次会话仍需 sudo，重新登录终端后会生效。"
  fi

  record_state_action docker_engine_linux
}

configure_linux_registry_mirrors() {
  local choice
  choice="$(ask_choice "Docker Hub 镜像配置处理方式：" "1" "1 2" \
    "1) 写入大陆镜像到 /etc/docker/daemon.json" \
    "2) 跳过，我自己配置或暂不配置")"
  if [ "${choice}" = "2" ]; then
    return 0
  fi

  run_sudo mkdir -p /etc/docker
  local mirrors_json
  mirrors_json="$(json_array_from_lines "${DOCKERHUB_MIRROR_CANDIDATES[@]}")"

  if [ -f /etc/docker/daemon.json ]; then
    local backup="/etc/docker/daemon.json.bak.$(date +%Y%m%d%H%M%S)"
    run_sudo cp /etc/docker/daemon.json "${backup}"
    log "已备份现有 daemon.json 到 ${backup}"
    set_state_value DOCKER_DAEMON_BACKUP "${backup}"
    set_state_value DOCKER_DAEMON_CREATED "0"

    if command_exists python3; then
      local tmp
      tmp="$(mktemp)"
      python3 - "$mirrors_json" /etc/docker/daemon.json > "${tmp}" <<'PY'
import json
import sys

mirrors = json.loads(sys.argv[1])
path = sys.argv[2]

with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)

data["registry-mirrors"] = mirrors
print(json.dumps(data, ensure_ascii=False, indent=2))
PY
      run_sudo cp "${tmp}" /etc/docker/daemon.json
      rm -f "${tmp}"
    else
      warn "未检测到 python3，已备份旧配置，将写入最小 daemon.json。"
      local tmp
      tmp="$(mktemp)"
      cat > "${tmp}" <<JSON
{
  "registry-mirrors": ${mirrors_json}
}
JSON
      run_sudo cp "${tmp}" /etc/docker/daemon.json
      rm -f "${tmp}"
    fi
  else
    set_state_value DOCKER_DAEMON_BACKUP ""
    set_state_value DOCKER_DAEMON_CREATED "1"
    local tmp
    tmp="$(mktemp)"
    cat > "${tmp}" <<JSON
{
  "registry-mirrors": ${mirrors_json}
}
JSON
    run_sudo cp "${tmp}" /etc/docker/daemon.json
    rm -f "${tmp}"
  fi

  if command_exists systemctl; then
    run_sudo systemctl daemon-reload || true
    run_sudo systemctl restart docker || true
  else
    run_sudo service docker restart || true
  fi

  record_common_state
  record_state_action docker_daemon_config
}

install_docker_macos() {
  log "准备在 macOS 上安装 Docker Desktop。"

  local choice
  if command_exists brew; then
    choice="$(ask_choice "检测到 Homebrew，请选择 Docker Desktop 安装方式：" "1" "1 2 3" \
      "1) 使用 Homebrew 安装：brew install --cask docker" \
      "2) 下载官方 DMG 并安装" \
      "3) 退出脚本，我自己安装")"
    case "${choice}" in
      1)
        brew install --cask docker
        set_state_value DOCKER_DESKTOP_INSTALL_METHOD "brew"
        ;;
      2)
        install_docker_macos_dmg
        set_state_value DOCKER_DESKTOP_INSTALL_METHOD "dmg"
        ;;
      3)
        die "已退出。请自行安装并启动 Docker 后重新运行脚本。"
        ;;
    esac
  else
    choice="$(ask_choice "未检测到 Homebrew，请选择 Docker Desktop 安装方式：" "1" "1 2" \
      "1) 下载官方 DMG 并安装" \
      "2) 退出脚本，我自己安装")"
    case "${choice}" in
      1)
        install_docker_macos_dmg
        set_state_value DOCKER_DESKTOP_INSTALL_METHOD "dmg"
        ;;
      2)
        die "已退出。请自行安装并启动 Docker 后重新运行脚本。"
        ;;
    esac
  fi

  log "启动 Docker Desktop。首次启动可能需要在图形界面确认授权。"
  record_common_state
  record_state_action docker_desktop_macos
  refresh_docker_path
  open -a Docker || true
}

install_docker_macos_dmg() {
  ensure_curl

  local arch url tmp_dir dmg mount_dir
  arch="$(uname -m)"
  case "${arch}" in
    arm64) url="https://desktop.docker.com/mac/main/arm64/Docker.dmg" ;;
    x86_64) url="https://desktop.docker.com/mac/main/amd64/Docker.dmg" ;;
    *) die "不支持的 macOS CPU 架构: ${arch}" ;;
  esac

  tmp_dir="$(mktemp -d)"
  dmg="${tmp_dir}/Docker.dmg"
  log "下载 Docker Desktop: ${url}"
  curl -L "${url}" -o "${dmg}"
  run_sudo hdiutil attach "${dmg}" -nobrowse
  mount_dir="/Volumes/Docker"
  run_sudo "${mount_dir}/Docker.app/Contents/MacOS/install" --accept-license --user="${USER}"
  run_sudo hdiutil detach "${mount_dir}"
  rm -rf "${tmp_dir}"
}

run_windows_powershell_bootstrap() {
  if ! command_exists powershell.exe; then
    return 1
  fi

  local ps_script="${SCRIPT_DIR}/demo-bootstrap.ps1"
  local windows_script="${ps_script}"

  if command_exists wslpath; then
    windows_script="$(wslpath -w "${ps_script}")"
  fi

  log "切换到 Windows PowerShell 安装入口。"
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "${windows_script}"
}

start_existing_docker() {
  local os_name="$1"

  case "${os_name}" in
    linux)
      if command_exists systemctl; then
        run_sudo systemctl start docker || true
      else
        run_sudo service docker start || true
      fi
      ;;
    macos)
      open -a Docker || true
      ;;
    wsl|windows-bash)
      run_windows_powershell_bootstrap || return 1
      ;;
    *)
      return 1
      ;;
  esac
}

ensure_docker() {
  local os_name="$1"

  local choice
  while true; do
    refresh_docker_path
    if command_exists docker && wait_for_docker; then
      log "Docker 已可用。"
      return 0
    fi

    if command_exists docker; then
      warn "检测到 docker 命令，但 Docker Engine 当前不可用。"
      choice="$(ask_choice "请选择 Docker Engine 处理方式：" "1" "1 2 3" \
        "1) 脚本尝试启动 Docker 并重新检测" \
        "2) 我已自己处理好，现在重新检测" \
        "3) 退出脚本，我自己处理")"
      case "${choice}" in
        1)
          start_existing_docker "${os_name}" || warn "脚本未能自动启动 Docker。"
          ;;
        2)
          ;;
        3)
          die "已退出。请自行处理 Docker 后重新运行脚本。"
          ;;
      esac
      continue
    fi

    choice="$(ask_choice "未检测到 Docker，请选择处理方式：" "1" "1 2 3" \
      "1) 脚本自动安装 Docker" \
      "2) 我已自己安装好，现在重新检测" \
      "3) 退出脚本，我自己安装")"
    case "${choice}" in
      1)
        case "${os_name}" in
          linux)
            install_docker_linux
            configure_linux_registry_mirrors
            ;;
          macos)
            install_docker_macos
            ;;
          wsl|windows-bash)
            if run_windows_powershell_bootstrap; then
              exit 0
            fi
            die "当前 Windows 兼容环境未检测到 powershell.exe，无法自动切换到 Windows 安装入口。"
            ;;
          *)
            die "无法自动安装 Docker：未知系统。"
            ;;
        esac
        ;;
      2)
        ;;
      3)
        die "已退出。请自行安装并启动 Docker 后重新运行脚本。"
        ;;
    esac
  done
}

resolve_compose_command() {
  local choice
  while true; do
    if docker compose version >/dev/null 2>&1; then
      printf 'docker compose'
      return 0
    fi

    if command_exists sudo && sudo -n docker compose version >/dev/null 2>&1; then
      printf 'sudo docker compose'
      return 0
    fi

    if command_exists docker-compose; then
      printf 'docker-compose'
      return 0
    fi

    choice="$(ask_choice "未检测到 Docker Compose，请选择处理方式：" "1" "1 2 3" \
      "1) 脚本自动安装或修复 Compose" \
      "2) 我已自己处理好，现在重新检测" \
      "3) 退出脚本，我自己处理")"
    case "${choice}" in
      1)
        install_compose_plugin_if_possible || warn "脚本未能自动安装或修复 Compose。"
        ;;
      2)
        ;;
      3)
        die "已退出。请自行安装 Docker Compose 后重新运行脚本。"
        ;;
    esac
  done
}

print_docker_status() {
  log "Docker 版本：$(docker --version)"
  if docker compose version >/dev/null 2>&1; then
    log "Compose 版本：$(docker compose version)"
  elif command_exists sudo && sudo -n docker compose version >/dev/null 2>&1; then
    log "Compose 版本：$(sudo docker compose version)"
  elif command_exists docker-compose; then
    log "Compose 版本：$(docker-compose --version)"
  fi
}

install_compose_plugin_if_possible() {
  local os_name
  os_name="$(detect_os)"

  case "${os_name}" in
    linux)
      log "未检测到 Docker Compose，尝试自动安装 docker-compose-plugin。"
      install_packages docker-compose-plugin
      record_common_state
      record_state_action docker_compose_plugin_linux
      ;;
    macos)
      if command_exists brew; then
        log "未检测到 Docker Compose，尝试使用 Homebrew 安装 docker-compose。"
        brew install docker-compose
        record_common_state
        record_state_action docker_compose_plugin_macos
      else
        warn "未检测到 Docker Compose，尝试重新启动 Docker Desktop。"
        open -a Docker || true
        sleep 5
      fi
      ;;
    *)
      return 1
      ;;
  esac
}

create_runtime_env_file() {
  [ -f "${ENV_FILE}" ] || die "缺少 ${ENV_FILE}"

  cleanup_runtime_env_file
  RUNTIME_ENV_FILE="$(mktemp)"
  grep -Ev '^(DOCKERHUB_LIBRARY_PREFIX|MYSQL_IMAGE|REDIS_IMAGE|WEB_PORT|SERVER_PORT|MYSQL_PORT|REDIS_PORT)=' "${ENV_FILE}" > "${RUNTIME_ENV_FILE}"

  local mysql_image="mysql:8.0"
  local redis_image="redis:7.4-alpine"
  if [ -n "${SELECTED_DOCKERHUB_LIBRARY_PREFIX}" ]; then
    mysql_image="${SELECTED_DOCKERHUB_LIBRARY_PREFIX}mysql:8.0"
    redis_image="${SELECTED_DOCKERHUB_LIBRARY_PREFIX}redis:7.4-alpine"
  fi

  {
    printf 'DOCKERHUB_LIBRARY_PREFIX=%s\n' "${SELECTED_DOCKERHUB_LIBRARY_PREFIX}"
    printf 'MYSQL_IMAGE=%s\n' "${mysql_image}"
    printf 'REDIS_IMAGE=%s\n' "${redis_image}"
    printf 'WEB_PORT=%s\n' "${WEB_PORT_VALUE:-$(get_base_env_value WEB_PORT 8080)}"
    printf 'SERVER_PORT=%s\n' "${SERVER_PORT_VALUE:-$(get_base_env_value SERVER_PORT 8089)}"
    printf 'MYSQL_PORT=%s\n' "${MYSQL_PORT_VALUE:-$(get_base_env_value MYSQL_PORT 13306)}"
    printf 'REDIS_PORT=%s\n' "${REDIS_PORT_VALUE:-$(get_base_env_value REDIS_PORT 16379)}"
  } >> "${RUNTIME_ENV_FILE}"
}

cleanup_runtime_env_file() {
  if [ -n "${RUNTIME_ENV_FILE}" ] && [ -f "${RUNTIME_ENV_FILE}" ]; then
    rm -f "${RUNTIME_ENV_FILE}"
  fi
}

get_env_value() {
  local key="$1"
  local file="${RUNTIME_ENV_FILE:-${ENV_FILE}}"
  grep "^${key}=" "${file}" | tail -n 1 | cut -d= -f2-
}

pull_runtime_images() {
  local compose_cmd="$1"

  log "拉取 MySQL 和 Redis 镜像。"
  # shellcheck disable=SC2086
  if ${compose_cmd} --env-file "${RUNTIME_ENV_FILE}" -f "${COMPOSE_FILE}" pull mysql redis; then
    record_common_state
    set_state_value MYSQL_IMAGE "$(get_env_value MYSQL_IMAGE)"
    set_state_value REDIS_IMAGE "$(get_env_value REDIS_IMAGE)"
    record_state_action runtime_images
    return 0
  fi

  warn "当前镜像源拉取 MySQL/Redis 失败，尝试其他镜像源。"

  local mirror prefix
  for mirror in "${DOCKERHUB_MIRROR_CANDIDATES[@]}"; do
    mirror="${mirror#"${mirror%%[![:space:]]*}"}"
    mirror="${mirror%"${mirror##*[![:space:]]}"}"
    [ -n "${mirror}" ] || continue
    prefix="$(mirror_to_library_prefix "${mirror}")"
    [ "${prefix}" != "${SELECTED_DOCKERHUB_LIBRARY_PREFIX}" ] || continue

    SELECTED_DOCKERHUB_LIBRARY_PREFIX="${prefix}"
    create_runtime_env_file
    log "改用 Docker Hub 镜像重新拉取: ${mirror}"
    # shellcheck disable=SC2086
    if ${compose_cmd} --env-file "${RUNTIME_ENV_FILE}" -f "${COMPOSE_FILE}" pull mysql redis; then
      record_common_state
      set_state_value MYSQL_IMAGE "$(get_env_value MYSQL_IMAGE)"
      set_state_value REDIS_IMAGE "$(get_env_value REDIS_IMAGE)"
      record_state_action runtime_images
      return 0
    fi
  done

  SELECTED_DOCKERHUB_LIBRARY_PREFIX=""
  create_runtime_env_file
  warn "大陆镜像拉取均失败，最后尝试官方 Docker Hub。"
  # shellcheck disable=SC2086
  ${compose_cmd} --env-file "${RUNTIME_ENV_FILE}" -f "${COMPOSE_FILE}" pull mysql redis
  record_common_state
  set_state_value MYSQL_IMAGE "$(get_env_value MYSQL_IMAGE)"
  set_state_value REDIS_IMAGE "$(get_env_value REDIS_IMAGE)"
  record_state_action runtime_images
}

run_project() {
  [ -f "${COMPOSE_FILE}" ] || die "缺少 ${COMPOSE_FILE}"

  local compose_cmd
  compose_cmd="$(resolve_compose_command)"
  resolve_host_ports
  create_runtime_env_file
  trap cleanup_runtime_env_file EXIT
  pull_runtime_images "${compose_cmd}"

  log "开始构建并启动项目环境。首次执行会下载基础镜像和依赖，耗时较长。"
  cd "${REPO_ROOT}"
  # shellcheck disable=SC2086
  ${compose_cmd} --env-file "${RUNTIME_ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build
  record_common_state
  record_state_action compose_project
  # shellcheck disable=SC2086
  ${compose_cmd} --env-file "${RUNTIME_ENV_FILE}" -f "${COMPOSE_FILE}" ps

  log "前端访问地址: http://localhost:$(get_env_value WEB_PORT)"
  log "后端 API 地址: http://localhost:$(get_env_value SERVER_PORT)"
  log "MySQL 本机端口: $(get_env_value MYSQL_PORT)，数据库: $(get_env_value MYSQL_DATABASE)"
}

main() {
  local os_name
  os_name="$(detect_os)"
  refresh_docker_path
  print_environment "${os_name}"
  ensure_docker "${os_name}"
  print_docker_status
  select_dockerhub_mirror

  log "项目镜像会使用脚本本次检测到的镜像源；如所有大陆镜像不可达，则回退官方 Docker Hub。"

  local choice
  choice="$(ask_choice "Docker 环境已就绪，请选择下一步：" "1" "1 2" \
    "1) 现在拉取镜像、构建并启动项目" \
    "2) 不启动，只输出后续命令")"
  if [ "${choice}" = "1" ]; then
    run_project
  else
    log "之后可运行：docker compose --env-file .env.demo -f compose.yaml up -d --build"
  fi
}

main "$@"
