#!/usr/bin/env bash
set -euo pipefail

# MariaDB/MySQL 真实方言验证入口；H2 不能代替本验证。
[ "${RUN_REAL_MIGRATION_TEST:-0}" = "1" ] || {
  printf '[SKIP] 设置 RUN_REAL_MIGRATION_TEST=1 后才运行真实 MariaDB/MySQL 迁移验证\n'; exit 0;
}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MYSQL_BIN="${CRM_MIGRATION_MYSQL_BIN:-mysql}"
HOST="${CRM_MIGRATION_DB_HOST:-127.0.0.1}"; PORT="${CRM_MIGRATION_DB_PORT:-3306}"
USER="${CRM_MIGRATION_DB_USERNAME:-}"; PASSWORD="${CRM_MIGRATION_DB_PASSWORD:-}"
SUFFIX="$(date +%Y%m%d%H%M%S)_$$"
LEGACY_DB="crm_migration_legacy_${SUFFIX}"
BASELINE_DB="crm_migration_baseline_${SUFFIX}"
[ -n "${USER}" ] || { printf '[ERROR] CRM_MIGRATION_DB_USERNAME 必填\n' >&2; exit 1; }
command -v "${MYSQL_BIN}" >/dev/null 2>&1 || { printf '[ERROR] 缺少数据库客户端\n' >&2; exit 1; }
mysql_admin() { MYSQL_PWD="${PASSWORD}" "${MYSQL_BIN}" --protocol=TCP --host="${HOST}" --port="${PORT}" --user="${USER}" --batch --raw "$@"; }
cleanup() {
  mysql_admin --execute "DROP DATABASE IF EXISTS \`${LEGACY_DB}\`; DROP DATABASE IF EXISTS \`${BASELINE_DB}\`" >/dev/null 2>&1 || true
}
trap cleanup EXIT

expect_injected_failure() {
  local step="$1"
  export CRM_MIGRATION_FAIL_AFTER_STEP="${step}"
  if "${SCRIPT_DIR}/user-management-migrate.sh" apply APPLY; then
    printf '[ERROR] 预期迁移在步骤 %s 中断，但命令成功\n' "${step}" >&2
    exit 1
  fi
  unset CRM_MIGRATION_FAIL_AFTER_STEP
}

expect_force_does_not_restore() {
  local migration_key="$1"
  local migration_file="$2"
  local damage_sql="$3"
  local probe_sql="$4"
  local expected="$5"
  local before after
  mysql_admin "${LEGACY_DB}" --execute "${damage_sql}"
  before="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "${probe_sql}" | tail -n 1)"
  [ "${before}" = "${expected}" ] || {
    printf '[ERROR] %s 的 --force 前置破坏未生效：期望 %s，实际 %s\n' "${migration_key}" "${expected}" "${before}" >&2
    exit 1
  }
  mysql_admin --force "${LEGACY_DB}" < "${REPO_ROOT}/dealer-server/src/main/resources/migration/${migration_file}" >/dev/null 2>&1 || true
  after="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "${probe_sql}" | tail -n 1)"
  [ "${after}" = "${before}" ] || {
    printf '[ERROR] mysql --force 绕过了 %s 的过程内 context：%s -> %s\n' "${migration_key}" "${before}" "${after}" >&2
    exit 1
  }
}

drop_check_constraint() {
  local database="$1" table_name="$2" constraint_name="$3" version
  version="$(mysql_admin "${database}" --skip-column-names --execute "SELECT VERSION()" | tail -n 1)"
  if [[ "${version}" == *MariaDB* ]]; then
    mysql_admin "${database}" --execute "ALTER TABLE ${table_name} DROP CONSTRAINT ${constraint_name}"
  else
    mysql_admin "${database}" --execute "ALTER TABLE ${table_name} DROP CHECK ${constraint_name}"
  fi
}

expect_sql_failure_message() {
  local database="$1" sql="$2" expected_message="$3" error_file status
  error_file="$(mktemp)"
  set +e
  mysql_admin "${database}" --execute "${sql}" >/dev/null 2>"${error_file}"
  status=$?
  set -e
  if [ "${status}" -eq 0 ] || ! grep -Fq "${expected_message}" "${error_file}"; then
    printf '[ERROR] SQL 未由预期数据库不变量拒绝：message=%s status=%s\n' "${expected_message}" "${status}" >&2
    cat "${error_file}" >&2
    rm -f "${error_file}"
    exit 1
  fi
  rm -f "${error_file}"
}

mysql_admin --execute "CREATE DATABASE \`${LEGACY_DB}\` CHARACTER SET utf8mb4"
mysql_admin "${LEGACY_DB}" < "${SCRIPT_DIR}/fixtures/user-management-pre-task03.sql"
export CRM_MIGRATION_DB_NAME="${LEGACY_DB}"

# 即使调用方显式使用 --force，Task03 的业务 DDL 仍由过程内 context 二次校验保护。
mysql_admin --force "${LEGACY_DB}" < "${REPO_ROOT}/dealer-server/src/main/resources/migration/20260711_task03_auth_version.sql" >/dev/null 2>&1 || true
direct_column_count="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${LEGACY_DB}' AND table_name='t_user' AND column_name='auth_version'" | tail -n 1)"
[ "${direct_column_count}" = "0" ] || { printf '[ERROR] --force 直接执行绕过了 Task03 context\n' >&2; exit 1; }

# 在三个含首跑回填的高风险迁移中真实注入 SQL 错误，再从步骤账本恢复。
expect_injected_failure AUTHORIZATION_COLUMNS_CONSTRAINTS_READY
failed_row_count="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM t_user_management_migration WHERE migration_key='20260711_task10_authorization_history' AND status='FAILED' AND last_completed_step='AUTHORIZATION_COLUMNS_CONSTRAINTS_READY'" | tail -n 1)"
[ "${failed_row_count}" = "1" ] || { printf '[ERROR] Task10 中断后未可靠记录 FAILED/step\n' >&2; exit 1; }
"${SCRIPT_DIR}/user-management-migrate.sh" resume 20260711_task10_authorization_history RESUME

expect_injected_failure ROLE_ORGANIZATION_TABLES_READY
"${SCRIPT_DIR}/user-management-migrate.sh" resume 20260711_task12_role_permission_matrix RESUME

expect_injected_failure AUTHORIZATION_VERSION_READY
"${SCRIPT_DIR}/user-management-migrate.sh" resume 20260711_task13_user_authorization RESUME

expect_injected_failure LOGIN_IDENTIFIER_BACKFILL_READY
login_identifier_step_rows="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM t_login_identifier li INNER JOIN t_user u ON u.id=li.user_id AND u.login_act=li.login_act WHERE li.status='ACTIVE' AND li.active_marker=1 AND li.retired_at IS NULL" | tail -n 1)"
user_rows="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM t_user" | tail -n 1)"
[ "${login_identifier_step_rows}" = "${user_rows}" ] || { printf '[ERROR] Task15 故障注入前未完整回填登录账号归属：%s/%s\n' "${login_identifier_step_rows}" "${user_rows}" >&2; exit 1; }
"${SCRIPT_DIR}/user-management-migrate.sh" resume 20260712_task15_credential_lifecycle RESUME

"${SCRIPT_DIR}/user-management-migrate.sh" apply APPLY
"${SCRIPT_DIR}/user-management-migrate.sh" verify
"${SCRIPT_DIR}/user-management-migrate.sh" apply APPLY

operation_log_history_index_definition="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT CONCAT(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX),':',MIN(NON_UNIQUE),':',MIN(INDEX_TYPE)) FROM information_schema.statistics WHERE table_schema='${LEGACY_DB}' AND table_name='t_operation_log' AND index_name='idx_operation_log_user_history'" | tail -n 1)"
[ "${operation_log_history_index_definition}" = "resource_id,action_code,create_time,id:1:BTREE" ] || {
  printf '[ERROR] 用户历史操作日志索引定义错误：%s\n' "${operation_log_history_index_definition}" >&2
  exit 1
}

state_rows="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT CONCAT(login_act,':',account_status,':',manual_locked) FROM t_user WHERE id IN (2,3,4) ORDER BY id" | tr '\n' ' ')"
[[ "${state_rows}" == *"disabled:DISABLED:0"* ]] || { printf '[ERROR] 旧禁用账号未保真迁移：%s\n' "${state_rows}" >&2; exit 1; }
[[ "${state_rows}" == *"locked:ACTIVE:1"* ]] || { printf '[ERROR] 旧锁定账号未保真迁移：%s\n' "${state_rows}" >&2; exit 1; }
[[ "${state_rows}" == *"normal:ACTIVE:0"* ]] || { printf '[ERROR] 旧正常账号迁移异常：%s\n' "${state_rows}" >&2; exit 1; }

identifier_rows="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM t_user u INNER JOIN t_login_identifier li ON li.user_id=u.id AND li.login_act=u.login_act AND li.status='ACTIVE' AND li.active_marker=1 AND li.retired_at IS NULL" | tail -n 1)"
[ "${identifier_rows}" = "${user_rows}" ] || { printf '[ERROR] 旧库登录账号永久归属不完整：%s/%s\n' "${identifier_rows}" "${user_rows}" >&2; exit 1; }
login_identifier_guard_rows="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM t_authorization_graph_lock WHERE lock_name='LOGIN_IDENTIFIER_GUARD'" | tail -n 1)"
[ "${login_identifier_guard_rows}" = "1" ] || { printf '[ERROR] LOGIN_IDENTIFIER_GUARD 未建立\n' >&2; exit 1; }
if mysql_admin "${LEGACY_DB}" --execute "START TRANSACTION; UPDATE t_login_identifier SET status='RETIRED',active_marker=NULL,retired_at=NOW(),version=version+1 WHERE login_act='normal'; INSERT INTO t_login_identifier(user_id,login_act,status,active_marker,retired_at,changed_by,reason,version,create_time) VALUES(2,'normal','ACTIVE',1,NULL,1,'非法转让测试',0,NOW()); ROLLBACK" >/dev/null 2>&1; then
  printf '[ERROR] 已退休登录账号仍可转让给其他用户\n' >&2; exit 1
fi

mysql_admin "${LEGACY_DB}" --execute "INSERT INTO t_authorization_history(subject_type,subject_id,change_type,role_id,reason,operator_id,occurred_time) VALUES('ROLE','1','CREATE',1,'真实迁移触发器测试',1,NOW())"
if mysql_admin "${LEGACY_DB}" --execute "UPDATE t_authorization_history SET reason='不得更新' WHERE subject_type='ROLE'" >/dev/null 2>&1; then
  printf '[ERROR] 授权历史 UPDATE 触发器未拒绝写入\n' >&2; exit 1
fi
if mysql_admin "${LEGACY_DB}" --execute "DELETE FROM t_authorization_history WHERE subject_type='ROLE'" >/dev/null 2>&1; then
  printf '[ERROR] 授权历史 DELETE 触发器未拒绝写入\n' >&2; exit 1
fi
mysql_admin "${LEGACY_DB}" --execute "INSERT INTO t_user_lifecycle_event(operation_id,request_id,action,user_id,employee_id,before_value,after_value,reason,operator_id,occurred_time) VALUES('REAL-MIGRATION-LIFECYCLE','REAL-MIGRATION','TRANSFER',2,1,'{}','{}','真实生命周期触发器测试',1,NOW())"
if mysql_admin "${LEGACY_DB}" --execute "UPDATE t_user_lifecycle_event SET reason='不得更新' WHERE operation_id='REAL-MIGRATION-LIFECYCLE'" >/dev/null 2>&1; then
  printf '[ERROR] 生命周期事件 UPDATE 触发器未拒绝写入\n' >&2; exit 1
fi
if mysql_admin "${LEGACY_DB}" --execute "DELETE FROM t_user_lifecycle_event WHERE operation_id='REAL-MIGRATION-LIFECYCLE'" >/dev/null 2>&1; then
  printf '[ERROR] 生命周期事件 DELETE 触发器未拒绝写入\n' >&2; exit 1
fi
mysql_admin "${LEGACY_DB}" --execute "INSERT INTO t_user(id,login_act,login_pwd,name,account_type,protected_account,account_status,version,authorization_version,auth_version,session_revision,profile_version,must_change_password,failed_login_count,manual_locked) VALUES(998,'trigger_target_998','x','触发器目标账号','HUMAN',0,'ACTIVE',0,0,0,0,0,0,0,0)"
expect_sql_failure_message "${LEGACY_DB}" "UPDATE t_login_identifier SET user_id=998 WHERE login_act='normal'" '登录标识永久归属字段禁止修改'
expect_sql_failure_message "${LEGACY_DB}" "UPDATE t_login_identifier SET login_act='normal-trigger-renamed' WHERE login_act='normal'" '登录标识永久归属字段禁止修改'
expect_sql_failure_message "${LEGACY_DB}" "DELETE FROM t_login_identifier WHERE login_act='normal'" '登录标识永久归属事实禁止删除'
expect_sql_failure_message "${LEGACY_DB}" "UPDATE t_user SET login_act='renamed-admin' WHERE id=1" '固定恢复账号身份禁止降级、转移或复制'
expect_sql_failure_message "${LEGACY_DB}" "UPDATE t_user SET account_type='SYSTEM',protected_account=1 WHERE id=2" '固定恢复账号身份禁止降级、转移或复制'
expect_sql_failure_message "${LEGACY_DB}" "INSERT INTO t_user(id,login_act,login_pwd,name,account_type,protected_account,account_status,version,authorization_version,auth_version,session_revision,profile_version,must_change_password,failed_login_count,manual_locked) VALUES(999,'second-recovery','x','第二恢复账号','SYSTEM',1,'ACTIVE',0,0,0,0,0,0,0,0)" '固定恢复账号身份禁止新增或复制'
expect_sql_failure_message "${LEGACY_DB}" "DELETE FROM t_user WHERE id=1" '受保护恢复账号禁止删除'

# 完整迁移后的每份业务脚本都制造一个可识别缺口，再用 mysql --force 直接执行。
# context 只能位于文件首行是不够的：--force 会继续执行，业务过程必须在首个变更前二次校验。
expect_force_does_not_restore \
  20260713_task19_operation_log_history_index 20260713_task19_operation_log_history_index.sql \
  "ALTER TABLE t_operation_log DROP INDEX idx_operation_log_user_history" \
  "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='${LEGACY_DB}' AND table_name='t_operation_log' AND index_name='idx_operation_log_user_history'" 0
expect_force_does_not_restore \
  20260713_task22_user_management_hardening 20260713_task22_user_management_hardening.sql \
  "DROP TRIGGER trg_login_identifier_immutable_bu; DROP TRIGGER trg_login_identifier_immutable_bd; DROP TRIGGER trg_recovery_account_identity_bi; DROP TRIGGER trg_recovery_account_identity_bu; DROP TRIGGER trg_recovery_account_identity_bd" \
  "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='${LEGACY_DB}' AND trigger_name IN ('trg_login_identifier_immutable_bu','trg_login_identifier_immutable_bd','trg_recovery_account_identity_bi','trg_recovery_account_identity_bu','trg_recovery_account_identity_bd')" 0
drop_check_constraint "${LEGACY_DB}" t_account_credential chk_account_credential_contact_binding
mysql_admin --force "${LEGACY_DB}" < "${REPO_ROOT}/dealer-server/src/main/resources/migration/20260713_task22_user_management_hardening.sql" >/dev/null 2>&1 || true
task22_contact_check_count="$(mysql_admin "${LEGACY_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM information_schema.check_constraints WHERE constraint_schema='${LEGACY_DB}' AND constraint_name='chk_account_credential_contact_binding'" | tail -n 1)"
[ "${task22_contact_check_count}" = "0" ] || { printf '[ERROR] mysql --force 绕过 Task22 context 恢复了联系方式绑定约束\n' >&2; exit 1; }
expect_force_does_not_restore \
  20260712_task20_user_lifecycle 20260712_task20_user_lifecycle.sql \
  "DELETE FROM t_authorization_graph_lock WHERE lock_name='TEST_DRIVE_SCHEDULE_GUARD'" \
  "SELECT COUNT(*) FROM t_authorization_graph_lock WHERE lock_name='TEST_DRIVE_SCHEDULE_GUARD'" 0
expect_force_does_not_restore \
  20260712_task20_user_lifecycle 20260712_task20_user_lifecycle.sql \
  "DROP TRIGGER trg_user_lifecycle_event_no_update; DROP TRIGGER trg_user_lifecycle_event_no_delete" \
  "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='${LEGACY_DB}' AND trigger_name IN ('trg_user_lifecycle_event_no_update','trg_user_lifecycle_event_no_delete')" 0
expect_force_does_not_restore \
  20260712_task19_user_history 20260712_task19_user_history.sql \
  "ALTER TABLE t_authorization_history DROP COLUMN affected_users_snapshot" \
  "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${LEGACY_DB}' AND table_name='t_authorization_history' AND column_name='affected_users_snapshot'" 0
expect_force_does_not_restore \
  20260712_task18_user_management_workspace 20260712_task18_user_management_workspace.sql \
  "ALTER TABLE t_user DROP INDEX idx_user_workspace_last_login" \
  "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='${LEGACY_DB}' AND table_name='t_user' AND index_name='idx_user_workspace_last_login'" 0
expect_force_does_not_restore \
  20260712_task17_user_session 20260712_task17_user_session.sql \
  "DROP TABLE t_user_session" \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${LEGACY_DB}' AND table_name='t_user_session'" 0
expect_force_does_not_restore \
  20260712_task16_profile 20260712_task16_profile.sql \
  "ALTER TABLE t_employee MODIFY COLUMN avatar_url VARCHAR(499) NULL" \
  "SELECT character_maximum_length FROM information_schema.columns WHERE table_schema='${LEGACY_DB}' AND table_name='t_employee' AND column_name='avatar_url'" 499
expect_force_does_not_restore \
  20260712_task15_credential_lifecycle 20260712_task15_credential_lifecycle.sql \
  "DROP TABLE t_login_identifier" \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${LEGACY_DB}' AND table_name='t_login_identifier'" 0
expect_force_does_not_restore \
  20260711_task13_user_authorization 20260711_task13_user_authorization.sql \
  "DROP TABLE t_user_permission_organization" \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${LEGACY_DB}' AND table_name='t_user_permission_organization'" 0
expect_force_does_not_restore \
  20260711_task12_role_permission_matrix 20260711_task12_role_permission_matrix.sql \
  "DELETE FROM t_authorization_graph_lock WHERE lock_name='REPORTING_GRAPH'" \
  "SELECT COUNT(*) FROM t_authorization_graph_lock WHERE lock_name='REPORTING_GRAPH'" 0
expect_force_does_not_restore \
  20260711_task11_organization_management 20260711_task11_organization_management.sql \
  "DELETE role_permission FROM t_role_permission role_permission INNER JOIN t_permission permission ON permission.id=role_permission.permission_id WHERE permission.code='organization:add'; DELETE FROM t_permission WHERE code='organization:add'" \
  "SELECT COUNT(*) FROM t_permission WHERE code='organization:add'" 0
expect_force_does_not_restore \
  20260711_task10_authorization_history 20260711_task10_authorization_history.sql \
  "ALTER TABLE t_operation_log MODIFY COLUMN detail VARCHAR(1024) NULL" \
  "SELECT character_maximum_length FROM information_schema.columns WHERE table_schema='${LEGACY_DB}' AND table_name='t_operation_log' AND column_name='detail'" 1024
expect_force_does_not_restore \
  20260711_task10_authorization_history 20260711_task10_authorization_history.sql \
  "DROP TRIGGER trg_authorization_history_no_update; DROP TRIGGER trg_authorization_history_no_delete" \
  "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='${LEGACY_DB}' AND trigger_name IN ('trg_authorization_history_no_update','trg_authorization_history_no_delete')" 0
expect_force_does_not_restore \
  20260711_task09_organization_foundation 20260711_task09_organization_foundation.sql \
  "DROP TABLE t_employee_reporting" \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${LEGACY_DB}' AND table_name='t_employee_reporting'" 0
# 完整初始化库必须走 baseline，且 baseline 不得改变核心业务行数。
mysql_admin --execute "CREATE DATABASE \`${BASELINE_DB}\` CHARACTER SET utf8mb4"
mysql_admin "${BASELINE_DB}" < "${REPO_ROOT}/dealer-server/src/main/resources/CarDealerCRM.sql"
baseline_bootstrap_rows="$(mysql_admin "${BASELINE_DB}" --skip-column-names --execute "SELECT COUNT(*) FROM t_organization_unit organization_unit INNER JOIN t_employee employee ON employee.id=organization_unit.leader_employee_id INNER JOIN t_user account ON account.id=employee.user_id AND account.login_act='limin' AND account.account_type='HUMAN' AND account.must_change_password=1 INNER JOIN t_user_role membership ON membership.user_id=account.id AND membership.active_marker=1 INNER JOIN t_role role_record ON role_record.id=membership.role_id AND role_record.role='admin' INNER JOIN t_employee_assignment assignment ON assignment.employee_id=employee.id AND assignment.assignment_type='PRIMARY' AND assignment.status='ACTIVE' AND assignment.active_primary_marker=1 INNER JOIN t_position position ON position.id=assignment.position_id AND position.enabled=1 AND position.code<>'UNASSIGNED_POSITION' WHERE organization_unit.code='DEFAULT_COMPANY' AND organization_unit.enabled=1 AND organization_unit.migration_placeholder=0 AND (employee.phone_verified=1 OR employee.email_verified=1)" | tail -n 1)"
[ "${baseline_bootstrap_rows}" = "1" ] || { printf '[ERROR] 完整库缺少根公司负责人及可完成首次安全设置的 HUMAN 管理员\n' >&2; exit 1; }
before_counts="$(mysql_admin "${BASELINE_DB}" --skip-column-names --execute "SELECT CONCAT((SELECT COUNT(*) FROM t_user),':',(SELECT COUNT(*) FROM t_user_role),':',(SELECT COUNT(*) FROM t_role_permission))" | tail -n 1)"
export CRM_MIGRATION_DB_NAME="${BASELINE_DB}"
"${SCRIPT_DIR}/user-management-migrate.sh" baseline BASELINE
"${SCRIPT_DIR}/user-management-migrate.sh" verify
after_counts="$(mysql_admin "${BASELINE_DB}" --skip-column-names --execute "SELECT CONCAT((SELECT COUNT(*) FROM t_user),':',(SELECT COUNT(*) FROM t_user_role),':',(SELECT COUNT(*) FROM t_role_permission))" | tail -n 1)"
[ "${before_counts}" = "${after_counts}" ] || { printf '[ERROR] baseline 改变业务行数：%s -> %s\n' "${before_counts}" "${after_counts}" >&2; exit 1; }

printf '[PASS] 旧库首跑、四处真实中断恢复、账号永久归属、状态保真、用户历史索引定义、逐脚本 --force 拒绝、三类不可变触发器、固定恢复身份、重复执行和完整库 baseline 均通过\n'
