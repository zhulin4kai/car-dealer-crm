#!/usr/bin/env bash
set -euo pipefail

# 用户管理人工升级的唯一显式入口。
# 本脚本不会被 Spring Boot、Compose 或任何应用启动流程自动调用。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MIGRATION_DIR="${REPO_ROOT}/dealer-server/src/main/resources/migration"
MANIFEST_FILE="${MIGRATION_DIR}/manifest.tsv"
LEDGER_BOOTSTRAP="${MIGRATION_DIR}/00000000_user_management_migration_ledger.sql"
EXECUTOR_VERSION="3"
LOCK_NAME="car_dealer_crm:user_management_migration"
FAIL_AFTER_STEP="${CRM_MIGRATION_FAIL_AFTER_STEP:-}"

MYSQL_BIN="${CRM_MIGRATION_MYSQL_BIN:-mysql}"
DB_HOST="${CRM_MIGRATION_DB_HOST:-127.0.0.1}"
DB_PORT="${CRM_MIGRATION_DB_PORT:-3306}"
DB_NAME="${CRM_MIGRATION_DB_NAME:-}"
DB_USERNAME="${CRM_MIGRATION_DB_USERNAME:-}"
DB_PASSWORD="${CRM_MIGRATION_DB_PASSWORD:-}"

SEQUENCES=()
KEYS=()
SCRIPTS=()
DEPENDENCIES=()
CHECKSUMS=()
RELEASE_STATES=()
BASELINE_MODES=()
RECOVERY_MODES=()
PROBES=()

log() { printf '[INFO] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'EOF'
用法：
  scripts/database/user-management-migrate.sh plan
  scripts/database/user-management-migrate.sh status
  scripts/database/user-management-migrate.sh apply APPLY
  scripts/database/user-management-migrate.sh resume <migration_key> RESUME
  scripts/database/user-management-migrate.sh baseline BASELINE
  scripts/database/user-management-migrate.sh verify

连接环境变量：
  CRM_MIGRATION_DB_HOST       默认 127.0.0.1
  CRM_MIGRATION_DB_PORT       默认 3306
  CRM_MIGRATION_DB_NAME       必填
  CRM_MIGRATION_DB_USERNAME   必填
  CRM_MIGRATION_DB_PASSWORD   可为空
  CRM_MIGRATION_MYSQL_BIN     默认 mysql，也可指定 mariadb

apply 只能执行尚无账本记录的迁移；RUNNING/FAILED 必须使用 resume。
baseline 只用于已由完整初始化脚本建立的数据库，并逐项执行 manifest probe 后绑定 checksum。
EOF
}

require_runtime() {
  command -v "${MYSQL_BIN}" >/dev/null 2>&1 || die "未找到数据库客户端：${MYSQL_BIN}"
  [ -n "${DB_NAME}" ] || die "必须设置 CRM_MIGRATION_DB_NAME"
  [ -n "${DB_USERNAME}" ] || die "必须设置 CRM_MIGRATION_DB_USERNAME"
  [[ "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]] || die "数据库名只能包含字母、数字和下划线"
  [[ "${DB_PORT}" =~ ^[0-9]{1,5}$ ]] || die "数据库端口非法"
  [ -f "${MANIFEST_FILE}" ] || die "迁移 manifest 不存在：${MANIFEST_FILE}"
  [ -f "${LEDGER_BOOTSTRAP}" ] || die "迁移账本 bootstrap 不存在：${LEDGER_BOOTSTRAP}"
  if [ -n "${FAIL_AFTER_STEP}" ]; then
    [[ "${FAIL_AFTER_STEP}" =~ ^[A-Z0-9_]{3,128}$ ]] || die "CRM_MIGRATION_FAIL_AFTER_STEP 非法"
  fi
}

mysql_command() {
  MYSQL_PWD="${DB_PASSWORD}" "${MYSQL_BIN}" \
    --protocol=TCP \
    --host="${DB_HOST}" \
    --port="${DB_PORT}" \
    --user="${DB_USERNAME}" \
    --database="${DB_NAME}" \
    --batch --raw --skip-column-names "$@"
}

query_scalar() {
  mysql_command --execute "$1" | tail -n 1
}

sha256_file() {
  local file="$1"
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "${file}" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${file}" | awk '{print $1}'
  else
    die "缺少 shasum 或 sha256sum，无法校验迁移 checksum"
  fi
}

valid_key() { [[ "$1" =~ ^[0-9]{8}_task[0-9]{2}_[a-z0-9_]+$ ]]; }
valid_checksum() { [[ "$1" =~ ^[0-9a-f]{64}$ ]]; }

load_manifest() {
  local sequence key script dependencies checksum release_state baseline_mode recovery_mode probe
  local previous_sequence=-1 numeric_sequence
  while IFS=$'\t' read -r sequence key script dependencies checksum release_state baseline_mode recovery_mode probe; do
    [ -n "${sequence}" ] || continue
    [[ "${sequence}" == \#* ]] && continue
    [[ "${sequence}" =~ ^[0-9]+$ ]] || die "manifest sequence 非法：${sequence}"
    numeric_sequence=$((10#${sequence}))
    [ "${numeric_sequence}" -gt "${previous_sequence}" ] || die "manifest sequence 必须严格递增：${sequence}"
    previous_sequence="${numeric_sequence}"
    valid_key "${key}" || die "manifest migration_key 非法：${key}"
    if index_of_key "${key}" >/dev/null 2>&1; then die "manifest migration_key 重复：${key}"; fi
    case "${release_state}" in
      ACTIVE)
        [[ "${script}" =~ ^[0-9]{8}_task[0-9]{2}_[a-z0-9_]+\.sql$ ]] || die "ACTIVE 迁移脚本名非法：${script}"
        [ -f "${MIGRATION_DIR}/${script}" ] || die "manifest 脚本不存在：${script}"
        valid_checksum "${checksum}" || die "manifest checksum 非法：${key}"
        [ -n "${probe}" ] && [ "${probe}" != "-" ] || die "ACTIVE 迁移缺少 baseline probe：${key}"
        [ "${baseline_mode}" = "PROBE_REQUIRED" ] || die "ACTIVE 迁移 baseline_mode 非法：${key}"
        case "${recovery_mode}" in
          OBJECT_DEFINITION_RESUME|FIRST_RUN_BACKFILL_THEN_OBJECT_RESUME) ;;
          *) die "ACTIVE 迁移 recovery_mode 非法：${key}" ;;
        esac
        ;;
      PENDING_INTEGRATION)
        [ "${script}" = "-" ] || die "待集成迁移不得提前绑定脚本：${key}"
        [ "${checksum}" = "-" ] || die "待集成迁移不得提前绑定 checksum：${key}"
        [ "${baseline_mode}" = "NOT_ALLOWED" ] || die "待集成迁移 baseline_mode 非法：${key}"
        ;;
      *) die "未知 release_state：${release_state}" ;;
    esac
    SEQUENCES+=("${sequence}")
    KEYS+=("${key}")
    SCRIPTS+=("${script}")
    DEPENDENCIES+=("${dependencies}")
    CHECKSUMS+=("${checksum}")
    RELEASE_STATES+=("${release_state}")
    BASELINE_MODES+=("${baseline_mode}")
    RECOVERY_MODES+=("${recovery_mode}")
    PROBES+=("${probe}")
  done < "${MANIFEST_FILE}"
  [ "${#KEYS[@]}" -gt 0 ] || die "manifest 为空"
  validate_manifest_dependencies
}

validate_manifest_dependencies() {
  local index dependency dependency_index old_ifs
  for index in "${!KEYS[@]}"; do
    [ "${DEPENDENCIES[$index]}" != "-" ] || continue
    old_ifs="${IFS}"; IFS=','
    for dependency in ${DEPENDENCIES[$index]}; do
      dependency_index="$(index_of_key "${dependency}")" || die "manifest 前置依赖不存在：${KEYS[$index]} -> ${dependency}"
      [ "${dependency_index}" -lt "${index}" ] || die "manifest 依赖顺序非法：${KEYS[$index]} -> ${dependency}"
      [ "${RELEASE_STATES[$dependency_index]}" = "ACTIVE" ] || die "manifest 不得依赖待集成迁移：${dependency}"
    done
    IFS="${old_ifs}"
  done
}

index_of_key() {
  local target="$1" index
  for index in "${!KEYS[@]}"; do
    if [ "${KEYS[$index]}" = "${target}" ]; then
      printf '%s\n' "${index}"
      return 0
    fi
  done
  return 1
}

manifest_checksum_for() {
  local index
  index="$(index_of_key "$1")" || die "manifest 缺少依赖：$1"
  [ "${RELEASE_STATES[$index]}" = "ACTIVE" ] || die "依赖尚未集成：$1"
  printf '%s\n' "${CHECKSUMS[$index]}"
}

verify_local_checksum() {
  local index="$1" expected actual
  [ "${RELEASE_STATES[$index]}" = "ACTIVE" ] || return 0
  expected="${CHECKSUMS[$index]}"
  actual="$(sha256_file "${MIGRATION_DIR}/${SCRIPTS[$index]}")"
  [ "${actual}" = "${expected}" ] || die "迁移脚本 checksum 漂移：${KEYS[$index]} expected=${expected} actual=${actual}"
}

# MySQL 8.4 不允许通过 PREPARE 在存储过程中创建触发器，而把 CREATE TRIGGER
# 直接写在迁移脚本顶层又会被 `mysql --force file.sql` 绕过入口 guard。迁移文件因此
# 只在普通块注释中保存触发器 payload；唯一执行器在同一持锁、RUNNING 会话中提取并执行。
# payload 不是独立可 SOURCE 文件，且仍被迁移文件 checksum 完整覆盖。
emit_guarded_runner_payloads() {
  local script_path="$1" key="$2"
  awk -v key="${key}" '
    $0 == "/* CRM_MIGRATION_RUNNER_PAYLOAD_BEGIN " key { inside=1; begins++; next }
    $0 == "CRM_MIGRATION_RUNNER_PAYLOAD_END " key " */" { if(!inside) exit 41; inside=0; ends++; next }
    inside { print }
    END {
      if (inside || begins != ends) exit 42;
      if (begins > 1) exit 43;
    }
  ' "${script_path}" || die "迁移 runner payload 标记非法：${key}"
}

ledger_exists() {
  [ "$(query_scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='t_user_management_migration'")" = "1" ]
}

modern_ledger_exists() {
  [ "$(query_scalar "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='t_user_management_migration' AND ((column_name='status' AND column_type='varchar(16)' AND is_nullable='NO') OR (column_name='checksum_sha256' AND column_type='char(64)') OR (column_name='last_completed_step' AND column_type='varchar(128)') OR (column_name='attempt_count' AND data_type='int' AND is_nullable='NO') OR (column_name='error_summary' AND column_type='varchar(1000)'))")" = "5" ] \
    && [ "$(query_scalar "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='t_user_management_migration_step' AND ((column_name='migration_key' AND column_type='varchar(128)' AND is_nullable='NO') OR (column_name='step_code' AND column_type='varchar(128)' AND is_nullable='NO') OR (column_name='completed_at' AND data_type='datetime' AND is_nullable='NO'))")" = "3" ] \
    && [ "$(query_scalar "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='t_user_management_migration_step' AND index_name='PRIMARY' AND column_name IN ('migration_key','step_code')")" = "2" ]
}

emit_lock_assertion() {
  local suffix="$1"
  cat <<SQL
DELIMITER \$\$
DROP PROCEDURE IF EXISTS crm_assert_migration_lock_${suffix}\$\$
CREATE PROCEDURE crm_assert_migration_lock_${suffix}()
BEGIN
  IF COALESCE(@crm_lock_acquired, 0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '用户管理迁移锁已被其他执行器占用';
  END IF;
END\$\$
CALL crm_assert_migration_lock_${suffix}()\$\$
DROP PROCEDURE crm_assert_migration_lock_${suffix}\$\$
DELIMITER ;
SQL
}

run_sql_stream() {
  local lock_timeout="${1:-0}" error_file status
  error_file="$(mktemp)"
  set +e
  mysql_command \
    --init-command="SET SESSION sql_mode=IF((@crm_lock_acquired:=GET_LOCK('${LOCK_NAME}',${lock_timeout}))=1,@@SESSION.sql_mode,'CRM_MIGRATION_LOCK_NOT_ACQUIRED')" \
    2>"${error_file}"
  status=$?
  set -e
  if [ "${status}" -ne 0 ]; then
    cat "${error_file}" >&2
  fi
  rm -f "${error_file}"
  return "${status}"
}

ensure_ledger() {
  local suffix="bootstrap_$$"
  {
    emit_lock_assertion "${suffix}"
    printf 'SOURCE %s\n' "${LEDGER_BOOTSTRAP}"
    printf "SELECT RELEASE_LOCK('%s');\n" "${LOCK_NAME}"
  } | run_sql_stream || die "迁移账本 bootstrap 失败"
}

ledger_row() {
  local key="$1"
  query_scalar "SELECT CONCAT_WS('|',status,COALESCE(checksum_sha256,''),COALESCE(last_completed_step,''),attempt_count) FROM t_user_management_migration WHERE migration_key='${key}'"
}

emit_dependency_guards() {
  local dependencies="$1" dependency checksum
  [ "${dependencies}" != "-" ] || return 0
  local old_ifs="${IFS}"
  IFS=','
  for dependency in ${dependencies}; do
    checksum="$(manifest_checksum_for "${dependency}")"
    cat <<SQL
  IF NOT EXISTS (
    SELECT 1 FROM t_user_management_migration
    WHERE migration_key='${dependency}' AND status='SUCCEEDED' AND checksum_sha256='${checksum}'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移前置依赖缺失、未成功或 checksum 不一致';
  END IF;
SQL
  done
  IFS="${old_ifs}"
}

emit_runtime_helpers() {
  cat <<'SQL'
DELIMITER $$
DROP PROCEDURE IF EXISTS crm_require_migration_context$$
CREATE PROCEDURE crm_require_migration_context(IN expected_key VARCHAR(128))
BEGIN
  IF COALESCE(@crm_migration_key, '') <> expected_key
     OR COALESCE(@crm_migration_checksum, '') = ''
     OR COALESCE(IS_USED_LOCK('car_dealer_crm:user_management_migration'), 0) <> CONNECTION_ID()
     OR NOT EXISTS (
       SELECT 1 FROM t_user_management_migration
       WHERE migration_key=expected_key AND status='RUNNING'
         AND checksum_sha256=@crm_migration_checksum
     ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移脚本只能由显式用户管理迁移执行器运行';
  END IF;
  SET @crm_migration_context_verified = 1;
END$$
DROP PROCEDURE IF EXISTS crm_migration_mark_step$$
CREATE PROCEDURE crm_migration_mark_step(IN expected_key VARCHAR(128), IN p_step_code VARCHAR(128))
BEGIN
  CALL crm_require_migration_context(expected_key);
  START TRANSACTION;
  INSERT INTO t_user_management_migration_step(migration_key,step_code,completed_at)
  VALUES(expected_key,p_step_code,NOW())
  ON DUPLICATE KEY UPDATE step_code=VALUES(step_code);
  SET @crm_migration_step_was_new = ROW_COUNT();
  IF @crm_migration_step_was_new = 1 THEN
    UPDATE t_user_management_migration
    SET last_completed_step=p_step_code
    WHERE migration_key=expected_key AND status='RUNNING'
      AND checksum_sha256=@crm_migration_checksum;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM t_user_management_migration_step
    WHERE migration_key=expected_key AND step_code=p_step_code
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移步骤账本更新失败';
  END IF;
  COMMIT;
  IF COALESCE(@crm_migration_fail_after_step, '') = p_step_code THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移测试故障注入';
  END IF;
END$$
DELIMITER ;
SQL
}

mark_failed() {
  local key="$1" checksum="$2" status="$3" suffix="failed_$$"
  {
    emit_lock_assertion "${suffix}"
    printf 'DROP PROCEDURE IF EXISTS crm_assert_migration_lock_run_%s;\n' "$$"
    printf 'DELIMITER $$\nDROP PROCEDURE IF EXISTS crm_mark_user_migration_failed$$\n'
    printf 'CREATE PROCEDURE crm_mark_user_migration_failed()\nBEGIN\n'
    printf "UPDATE t_user_management_migration SET status='FAILED',failed_at=NOW(),completed_at=NULL,error_summary='mysql client exit %s; inspect executor stderr',executor_version='%s' WHERE migration_key='%s' AND status='RUNNING' AND checksum_sha256='%s';\n" \
      "${status}" "${EXECUTOR_VERSION}" "${key}" "${checksum}"
    printf "IF ROW_COUNT() <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='迁移失败状态账本更新影响行数异常'; END IF;\n"
    printf 'END$$\nCALL crm_mark_user_migration_failed()$$\nDROP PROCEDURE crm_mark_user_migration_failed$$\nDELIMITER ;\n'
    printf "SELECT RELEASE_LOCK('%s');\n" "${LOCK_NAME}"
  } | run_sql_stream 5 || warn "无法把迁移失败状态写入账本：${key}"
}

execute_migration() {
  local index="$1" mode="$2"
  local key="${KEYS[$index]}" checksum="${CHECKSUMS[$index]}" script_path row state current_checksum
  script_path="${MIGRATION_DIR}/${SCRIPTS[$index]}"
  verify_local_checksum "${index}"
  row="$(ledger_row "${key}")"
  state="${row%%|*}"
  current_checksum=""
  if [ -n "${row}" ]; then
    current_checksum="${row#*|}"
    current_checksum="${current_checksum%%|*}"
  fi

  if [ "${mode}" = "apply" ] && [ "${state}" = "SUCCEEDED" ] && [ "${current_checksum}" = "${checksum}" ]; then
    log "跳过已完成迁移：${key}"
    return 0
  fi
  if [ "${mode}" = "apply" ] && [ -n "${row}" ]; then
    die "${key} 已有状态 ${state:-UNKNOWN}；checksum 缺失/漂移或未完成时禁止 apply，请先 status/verify，RUNNING/FAILED 使用 resume"
  fi
  if [ "${mode}" = "resume" ]; then
    [ "${state}" = "RUNNING" ] || [ "${state}" = "FAILED" ] || die "${key} 不是 RUNNING/FAILED，不能 resume"
    [ "${current_checksum}" = "${checksum}" ] || die "${key} 的失败尝试 checksum 已漂移，禁止 resume"
  fi

  local suffix="run_$$" sql_status
  log "$([ "${mode}" = "resume" ] && printf '恢复' || printf '执行')迁移：${key}"
  set +e
  {
    emit_lock_assertion "${suffix}"
    printf 'SOURCE %s\n' "${LEDGER_BOOTSTRAP}"
    printf "SET @crm_migration_key='%s', @crm_migration_checksum='%s', @crm_migration_context_verified=0, @crm_migration_fail_after_step='%s';\n" \
      "${key}" "${checksum}" "${FAIL_AFTER_STEP}"
    emit_runtime_helpers
    printf 'DELIMITER $$\nDROP PROCEDURE IF EXISTS crm_prepare_user_migration$$\n'
    printf 'CREATE PROCEDURE crm_prepare_user_migration()\nBEGIN\n'
    emit_dependency_guards "${DEPENDENCIES[$index]}"
    if [ "${mode}" = "apply" ]; then
      cat <<SQL
  IF EXISTS (SELECT 1 FROM t_user_management_migration WHERE migration_key='${key}') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移已有账本记录，禁止重复 apply';
  END IF;
  INSERT INTO t_user_management_migration
    (migration_key,status,checksum_sha256,started_at,completed_at,failed_at,last_completed_step,attempt_count,error_summary,executor_version)
  VALUES
    ('${key}','RUNNING','${checksum}',NOW(),NULL,NULL,'STARTED',1,NULL,'${EXECUTOR_VERSION}');
SQL
    else
      cat <<SQL
  IF NOT EXISTS (
    SELECT 1 FROM t_user_management_migration
    WHERE migration_key='${key}' AND status IN ('RUNNING','FAILED') AND checksum_sha256='${checksum}'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移状态或 checksum 不允许 resume';
  END IF;
  UPDATE t_user_management_migration
  SET status='RUNNING',started_at=NOW(),failed_at=NULL,error_summary=NULL,
      last_completed_step=COALESCE(last_completed_step,'STARTED'),
      attempt_count=attempt_count+1,executor_version='${EXECUTOR_VERSION}'
  WHERE migration_key='${key}';
SQL
    fi
    printf 'END$$\nCALL crm_prepare_user_migration()$$\nDROP PROCEDURE crm_prepare_user_migration$$\nDELIMITER ;\n'
    printf 'SOURCE %s\n' "${script_path}"
    printf "CALL crm_require_migration_context('%s');\n" "${key}"
    emit_guarded_runner_payloads "${script_path}" "${key}"
    printf "CALL crm_migration_mark_step('%s','SCRIPT_EXECUTED');\n" "${key}"
    printf 'DELIMITER $$\nDROP PROCEDURE IF EXISTS crm_finalize_user_migration$$\n'
    printf 'CREATE PROCEDURE crm_finalize_user_migration()\nBEGIN\n'
    printf "CALL crm_require_migration_context('%s');\n" "${key}"
    printf "IF NOT (%s) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='迁移对象定义核验失败'; END IF;\n" "${PROBES[$index]}"
    cat <<SQL
UPDATE t_user_management_migration
SET status='SUCCEEDED',completed_at=NOW(),failed_at=NULL,last_completed_step='SUCCEEDED',
    error_summary=NULL,executor_version='${EXECUTOR_VERSION}'
WHERE migration_key='${key}' AND status='RUNNING' AND checksum_sha256='${checksum}';
IF ROW_COUNT() <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='迁移成功状态账本更新影响行数异常'; END IF;
END\$\$
CALL crm_finalize_user_migration()\$\$
DROP PROCEDURE crm_finalize_user_migration\$\$
DELIMITER ;
SELECT RELEASE_LOCK('${LOCK_NAME}');
SQL
  } | run_sql_stream
  sql_status=$?
  set -e
  if [ "${sql_status}" -ne 0 ]; then
    mark_failed "${key}" "${checksum}" "${sql_status}"
    die "迁移失败：${key}"
  fi
}

baseline_entry() {
  local index="$1"
  local key="${KEYS[$index]}" checksum="${CHECKSUMS[$index]}" row state current_checksum suffix="baseline_$$"
  verify_local_checksum "${index}"
  row="$(ledger_row "${key}")"
  state="${row%%|*}"
  current_checksum=""
  if [ -n "${row}" ]; then
    current_checksum="${row#*|}"
    current_checksum="${current_checksum%%|*}"
  fi
  if [ "${state}" = "SUCCEEDED" ] && [ "${current_checksum}" = "${checksum}" ]; then
    log "baseline 已绑定：${key}"
    return 0
  fi
  [ "${state}" != "RUNNING" ] && [ "${state}" != "FAILED" ] || die "${key} 为 ${state}，禁止 baseline 覆盖执行事实"
  {
    emit_lock_assertion "${suffix}"
    printf 'SOURCE %s\n' "${LEDGER_BOOTSTRAP}"
    printf 'DELIMITER $$\nDROP PROCEDURE IF EXISTS crm_baseline_user_migration$$\n'
    printf 'CREATE PROCEDURE crm_baseline_user_migration()\nBEGIN\n'
    emit_dependency_guards "${DEPENDENCIES[$index]}"
    printf "  IF NOT (%s) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'baseline 对象定义核验失败'; END IF;\n" "${PROBES[$index]}"
    cat <<SQL
  IF EXISTS (
    SELECT 1 FROM t_user_management_migration
    WHERE migration_key='${key}' AND checksum_sha256 IS NOT NULL AND checksum_sha256<>'${checksum}'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'baseline checksum 与已有账本冲突';
  END IF;
  INSERT INTO t_user_management_migration
    (migration_key,status,checksum_sha256,started_at,completed_at,failed_at,last_completed_step,attempt_count,error_summary,executor_version)
  VALUES
    ('${key}','SUCCEEDED','${checksum}',NOW(),NOW(),NULL,'BASELINE_VERIFIED',0,NULL,'${EXECUTOR_VERSION}')
  ON DUPLICATE KEY UPDATE status='SUCCEEDED',checksum_sha256=VALUES(checksum_sha256),
    completed_at=COALESCE(completed_at,NOW()),failed_at=NULL,last_completed_step='BASELINE_VERIFIED',
    error_summary=NULL,executor_version=VALUES(executor_version);
  INSERT INTO t_user_management_migration_step(migration_key,step_code,completed_at)
  VALUES('${key}','BASELINE_VERIFIED',NOW())
  ON DUPLICATE KEY UPDATE step_code=VALUES(step_code);
END\$\$
CALL crm_baseline_user_migration()\$\$
DROP PROCEDURE crm_baseline_user_migration\$\$
DELIMITER ;
SELECT RELEASE_LOCK('${LOCK_NAME}');
SQL
  } | run_sql_stream || die "baseline 失败：${key}"
  log "baseline 已验证并绑定：${key}"
}

show_plan() {
  local modern=0 index row state checksum actual
  if ledger_exists && modern_ledger_exists; then modern=1; fi
  printf '%-4s %-48s %-20s %s\n' 序号 migration_key 状态 恢复策略
  for index in "${!KEYS[@]}"; do
    if [ "${RELEASE_STATES[$index]}" != "ACTIVE" ]; then
      printf '%-4s %-48s %-20s %s\n' "${SEQUENCES[$index]}" "${KEYS[$index]}" PENDING_INTEGRATION "${RECOVERY_MODES[$index]}"
      continue
    fi
    actual="$(sha256_file "${MIGRATION_DIR}/${SCRIPTS[$index]}")"
    if [ "${actual}" != "${CHECKSUMS[$index]}" ]; then
      state="LOCAL_CHECKSUM_DRIFT"
    elif [ "${modern}" -eq 0 ]; then
      state="PENDING_LEDGER_BOOTSTRAP"
    else
      row="$(ledger_row "${KEYS[$index]}")"
      state="${row%%|*}"
      checksum=""
      if [ -n "${row}" ]; then checksum="${row#*|}"; checksum="${checksum%%|*}"; fi
      if [ -z "${row}" ]; then state="PENDING";
      elif [ "${state}" = "SUCCEEDED" ] && [ "${checksum}" != "${CHECKSUMS[$index]}" ]; then state="CHECKSUM_MISMATCH";
      fi
    fi
    printf '%-4s %-48s %-20s %s\n' "${SEQUENCES[$index]}" "${KEYS[$index]}" "${state}" "${RECOVERY_MODES[$index]}"
  done
}

verify_all() {
  modern_ledger_exists || die "统一迁移账本尚未 bootstrap"
  local index row state checksum probe_result ledger_integrity
  for index in "${!KEYS[@]}"; do
    [ "${RELEASE_STATES[$index]}" = "ACTIVE" ] || continue
    verify_local_checksum "${index}"
    row="$(ledger_row "${KEYS[$index]}")"
    state="${row%%|*}"
    checksum="${row#*|}"; checksum="${checksum%%|*}"
    [ "${state}" = "SUCCEEDED" ] || die "迁移未成功：${KEYS[$index]} status=${state:-ABSENT}"
    [ "${checksum}" = "${CHECKSUMS[$index]}" ] || die "账本 checksum 不一致：${KEYS[$index]}"
    ledger_integrity="$(query_scalar "SELECT COUNT(*) FROM t_user_management_migration WHERE migration_key='${KEYS[$index]}' AND status='SUCCEEDED' AND completed_at IS NOT NULL AND failed_at IS NULL AND ((last_completed_step='SUCCEEDED' AND attempt_count>=1) OR (last_completed_step='BASELINE_VERIFIED' AND attempt_count=0))")"
    [ "${ledger_integrity}" = "1" ] || die "迁移账本状态组合非法：${KEYS[$index]}"
    probe_result="$(query_scalar "SELECT IF(${PROBES[$index]},1,0)")"
    [ "${probe_result}" = "1" ] || die "对象定义核验失败：${KEYS[$index]}"
  done
  log "全部已集成用户管理迁移的账本、checksum 和对象 probe 均通过"
}

main() {
  local command="${1:-help}"
  case "${command}" in help|-h|--help) usage; exit 0 ;; esac
  require_runtime
  load_manifest
  case "${command}" in
    plan|status)
      show_plan
      ;;
    apply)
      [ "${2:-}" = "APPLY" ] || die "apply 必须显式追加 APPLY，并确认已停止写入和完成备份"
      ensure_ledger
      local index
      for index in "${!KEYS[@]}"; do
        [ "${RELEASE_STATES[$index]}" = "ACTIVE" ] || continue
        execute_migration "${index}" apply
      done
      verify_all
      ;;
    resume)
      [ -n "${2:-}" ] || die "resume 必须指定 migration_key"
      [ "${3:-}" = "RESUME" ] || die "resume 必须显式追加 RESUME"
      ensure_ledger
      local resume_index
      resume_index="$(index_of_key "$2")" || die "manifest 不存在迁移：$2"
      [ "${RELEASE_STATES[$resume_index]}" = "ACTIVE" ] || die "Task20 尚待集成，不能 resume"
      execute_migration "${resume_index}" resume
      ;;
    baseline)
      [ "${2:-}" = "BASELINE" ] || die "baseline 必须显式追加 BASELINE"
      ensure_ledger
      local baseline_index
      for baseline_index in "${!KEYS[@]}"; do
        [ "${RELEASE_STATES[$baseline_index]}" = "ACTIVE" ] || continue
        baseline_entry "${baseline_index}"
      done
      verify_all
      ;;
    verify)
      verify_all
      ;;
    *) usage; die "未知命令：${command}" ;;
  esac
}

main "$@"
