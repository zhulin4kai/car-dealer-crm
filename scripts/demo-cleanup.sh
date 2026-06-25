#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env.demo"
COMPOSE_FILE="${REPO_ROOT}/compose.yaml"
STATE_DIR="${HOME}/.car-dealer-crm-demo"
STATE_ACTIONS_FILE="${STATE_DIR}/actions.txt"
STATE_ENV_FILE="${STATE_DIR}/state.env"

CANDIDATE_KEYS=()
CANDIDATE_LABELS=()
SELECTED_KEYS=()

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

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

run_sudo() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
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

state_has_action() {
  local action="$1"
  [ -f "${STATE_ACTIONS_FILE}" ] && grep -qxF "${action}" "${STATE_ACTIONS_FILE}"
}

get_state_value() {
  local key="$1"
  local default_value="${2:-}"
  if [ -f "${STATE_ENV_FILE}" ]; then
    local value
    value="$(grep "^${key}=" "${STATE_ENV_FILE}" | tail -n 1 | cut -d= -f2- || true)"
    if [ -n "${value}" ]; then
      printf '%s\n' "${value}"
      return
    fi
  fi
  printf '%s\n' "${default_value}"
}

get_env_value() {
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

remove_state_action() {
  local action="$1"
  [ -f "${STATE_ACTIONS_FILE}" ] || return 0
  local tmp
  tmp="$(mktemp)"
  grep -vxF "${action}" "${STATE_ACTIONS_FILE}" > "${tmp}" || true
  mv "${tmp}" "${STATE_ACTIONS_FILE}"
}

add_candidate() {
  CANDIDATE_KEYS+=("$1")
  CANDIDATE_LABELS+=("$2")
}

resolve_compose_command() {
  if command_exists docker && docker compose version >/dev/null 2>&1; then
    printf 'docker compose'
    return 0
  fi
  if command_exists docker-compose; then
    printf 'docker-compose'
    return 0
  fi
  return 1
}

collect_candidates() {
  local compose_cmd=""
  if compose_cmd="$(resolve_compose_command 2>/dev/null)" && [ -f "${COMPOSE_FILE}" ]; then
    add_candidate "compose_project" "项目容器、网络、数据卷和本地构建镜像（${compose_cmd} down --volumes --rmi local）"
  fi

  if state_has_action runtime_images; then
    add_candidate "runtime_images" "启动脚本拉取过的 MySQL/Redis 镜像"
  fi

  if state_has_action docker_daemon_config; then
    add_candidate "docker_daemon_config" "启动脚本写入的 Linux Docker daemon 镜像配置"
  fi

  if state_has_action docker_compose_plugin_linux || state_has_action docker_compose_plugin_macos; then
    add_candidate "docker_compose_plugin" "启动脚本安装的 Docker Compose 插件或 docker-compose"
  fi

  if state_has_action docker_desktop_macos; then
    add_candidate "docker_desktop_macos" "启动脚本安装的 Docker Desktop for macOS"
  fi

  if state_has_action docker_engine_linux; then
    add_candidate "docker_engine_linux" "启动脚本安装的 Docker Engine for Linux"
  fi

  if [ -d "${STATE_DIR}" ]; then
    add_candidate "install_record" "安装记录目录 ${STATE_DIR}"
  fi
}

print_candidates() {
  if [ "${#CANDIDATE_KEYS[@]}" -eq 0 ]; then
    log "未发现可由脚本定位的清理项。"
    exit 0
  fi

  printf '即将可删除的项目如下：\n'
  local index
  for index in "${!CANDIDATE_KEYS[@]}"; do
    printf '  %s) %s\n' "$((index + 1))" "${CANDIDATE_LABELS[$index]}"
  done
}

choose_candidates() {
  print_candidates
  printf '\n'
  printf '请选择删除范围：\n'
  printf '  1) 删除全部\n'
  printf '  2) 选择部分删除\n'
  printf '  3) 退出\n'

  local choice
  while true; do
    read -r -p "请选择 [3]: " choice
    choice="${choice:-3}"
    case "${choice}" in
      1)
        SELECTED_KEYS=("${CANDIDATE_KEYS[@]}")
        return
        ;;
      2)
        choose_partial_candidates
        return
        ;;
      3)
        exit 0
        ;;
      *)
        printf '请输入 1、2 或 3。\n'
        ;;
    esac
  done
}

choose_partial_candidates() {
  local answer
  while true; do
    read -r -p "请输入要删除的编号，用英文逗号分隔，例如 1,3: " answer
    [ -n "${answer}" ] || continue

    SELECTED_KEYS=()
    local item
    local valid=1
    IFS=',' read -r -a parts <<< "${answer}"
    for item in "${parts[@]}"; do
      item="${item//[[:space:]]/}"
      if [[ ! "${item}" =~ ^[0-9]+$ ]] || [ "${item}" -lt 1 ] || [ "${item}" -gt "${#CANDIDATE_KEYS[@]}" ]; then
        valid=0
        break
      fi
      SELECTED_KEYS+=("${CANDIDATE_KEYS[$((item - 1))]}")
    done

    if [ "${valid}" -eq 1 ] && [ "${#SELECTED_KEYS[@]}" -gt 0 ]; then
      return
    fi
    printf '编号无效，请重新输入。\n'
  done
}

confirm_deletion() {
  printf '\n将删除以下内容：\n'
  local key index
  for key in "${SELECTED_KEYS[@]}"; do
    for index in "${!CANDIDATE_KEYS[@]}"; do
      if [ "${CANDIDATE_KEYS[$index]}" = "${key}" ]; then
        printf '  - %s\n' "${CANDIDATE_LABELS[$index]}"
      fi
    done
  done

  printf '\n这是不可逆操作。请输入 DELETE 确认删除: '
  local answer
  read -r answer
  if [ "${answer}" != "DELETE" ]; then
    die "未确认删除，已退出。"
  fi
}

remove_compose_project() {
  local compose_cmd
  if ! compose_cmd="$(resolve_compose_command)"; then
    warn "未检测到 Docker Compose，跳过项目容器清理。"
    return
  fi
  [ -f "${COMPOSE_FILE}" ] || return 0
  # shellcheck disable=SC2086
  ${compose_cmd} --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" down --volumes --remove-orphans --rmi local
  remove_state_action compose_project
}

remove_runtime_images() {
  command_exists docker || {
    warn "未检测到 docker 命令，跳过镜像删除。"
    return
  }

  local mysql_image redis_image
  mysql_image="$(get_state_value MYSQL_IMAGE "$(get_env_value MYSQL_IMAGE "")")"
  redis_image="$(get_state_value REDIS_IMAGE "$(get_env_value REDIS_IMAGE "")")"

  [ -n "${mysql_image}" ] && docker image rm "${mysql_image}" || true
  [ -n "${redis_image}" ] && docker image rm "${redis_image}" || true
  remove_state_action runtime_images
}

restart_docker_if_possible() {
  if command_exists systemctl; then
    run_sudo systemctl restart docker || true
  else
    run_sudo service docker restart || true
  fi
}

remove_docker_daemon_config() {
  local backup created
  backup="$(get_state_value DOCKER_DAEMON_BACKUP "")"
  created="$(get_state_value DOCKER_DAEMON_CREATED "0")"

  if [ -n "${backup}" ] && [ -f "${backup}" ]; then
    run_sudo cp "${backup}" /etc/docker/daemon.json
    log "已恢复 Docker daemon 配置备份: ${backup}"
  elif [ "${created}" = "1" ] && [ -f /etc/docker/daemon.json ]; then
    run_sudo rm -f /etc/docker/daemon.json
    log "已删除启动脚本创建的 /etc/docker/daemon.json"
  else
    warn "未找到可恢复的 Docker daemon 配置记录。"
  fi

  restart_docker_if_possible
  remove_state_action docker_daemon_config
}

remove_package_if_possible() {
  if command_exists apt-get; then
    run_sudo apt-get remove -y "$@" || true
  elif command_exists dnf; then
    run_sudo dnf remove -y "$@" || true
  elif command_exists yum; then
    run_sudo yum remove -y "$@" || true
  elif command_exists pacman; then
    run_sudo pacman -Rns --noconfirm "$@" || true
  elif command_exists zypper; then
    run_sudo zypper --non-interactive remove "$@" || true
  else
    warn "无法识别包管理器，跳过软件包卸载。"
  fi
}

remove_compose_plugin() {
  local os_name
  os_name="$(detect_os)"
  case "${os_name}" in
    linux)
      remove_package_if_possible docker-compose-plugin
      remove_state_action docker_compose_plugin_linux
      ;;
    macos)
      if command_exists brew; then
        brew uninstall docker-compose || true
      else
        warn "未检测到 Homebrew，跳过 docker-compose 卸载。"
      fi
      remove_state_action docker_compose_plugin_macos
      ;;
  esac
}

remove_docker_desktop_macos() {
  if command_exists brew && [ "$(get_state_value DOCKER_DESKTOP_INSTALL_METHOD "")" = "brew" ]; then
    brew uninstall --cask docker || true
  elif [ -x /Applications/Docker.app/Contents/MacOS/uninstall ]; then
    /Applications/Docker.app/Contents/MacOS/uninstall || true
  else
    run_sudo rm -rf /Applications/Docker.app
  fi
  remove_state_action docker_desktop_macos
}

remove_docker_engine_linux() {
  remove_package_if_possible docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  remove_state_action docker_engine_linux
}

remove_install_record() {
  rm -rf "${STATE_DIR}"
}

execute_cleanup() {
  local key
  for key in "${SELECTED_KEYS[@]}"; do
    case "${key}" in
      compose_project) remove_compose_project ;;
      runtime_images) remove_runtime_images ;;
      docker_daemon_config) remove_docker_daemon_config ;;
      docker_compose_plugin) remove_compose_plugin ;;
      docker_desktop_macos) remove_docker_desktop_macos ;;
      docker_engine_linux) remove_docker_engine_linux ;;
      install_record) remove_install_record ;;
    esac
  done
}

main() {
  local os_name
  os_name="$(detect_os)"
  if [ "${os_name}" = "wsl" ] || [ "${os_name}" = "windows-bash" ]; then
    if command_exists powershell.exe; then
      local ps_script="${SCRIPT_DIR}/demo-cleanup.ps1"
      local windows_script="${ps_script}"
      command_exists wslpath && windows_script="$(wslpath -w "${ps_script}")"
      powershell.exe -NoProfile -ExecutionPolicy Bypass -File "${windows_script}"
      exit $?
    fi
  fi

  collect_candidates
  choose_candidates
  confirm_deletion
  execute_cleanup
  log "清理完成。"
}

main "$@"
