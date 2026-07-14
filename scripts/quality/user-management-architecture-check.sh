#!/usr/bin/env bash

set -euo pipefail

usage() {
    cat <<'EOF'
用法：
  scripts/quality/user-management-architecture-check.sh --baseline
  scripts/quality/user-management-architecture-check.sh --current
  scripts/quality/user-management-architecture-check.sh --compare-baseline

说明：
  --baseline          输出并校验 Task 25 冻结的本轮开始 HEAD 快照。
  --current           按 Task 33 的净减少与调用边界门禁检查当前实现。
  --compare-baseline  与 --current 等价，供 Task 33 验收命令使用。

脚本只读取 Java 源文件，不创建、格式化或修改任何仓库文件。
EOF
}

if [[ $# -ne 1 ]]; then
    usage
    exit 2
fi

case "$1" in
    --baseline)
        MODE="baseline"
        ;;
    --current|--compare-baseline)
        MODE="current"
        ;;
    -h|--help)
        usage
        exit 0
        ;;
    *)
        usage
        exit 2
        ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

command -v rg >/dev/null 2>&1 || {
    echo "ERROR: 需要 rg 执行只读架构统计" >&2
    exit 2
}

FIXED_FILES=(
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/AuthorizationServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/CredentialServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/LoginSecurityServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/ManagedUserAccountServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/ManagedUserInvitationServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/OrganizationServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/ProfileServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/RoleAccessServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/UserLifecycleServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/UserSessionServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/UserServiceImpl.java"
    "dealer-server/src/main/java/com/autodealer/crm/service/impl/AdminAccessRecoveryService.java"
)

BASELINE_COMMIT="d0d91f7f7f9d96389df909d22798ce8cceff3237"
BASELINE_FIXED_LOCS=(355 107 63 824 420 1297 185 201 492 226 753 102)
BASELINE_EXISTING_PRODUCTION_LOC=5025

if [[ "$MODE" == "baseline" ]]; then
    frozen_total=0
    echo "mode=baseline"
    echo "baseline_semantics=frozen_round_start_HEAD_snapshot_not_current_worktree"
    echo "baseline_commit=$BASELINE_COMMIT"
    printf '%-104s %8s\n' "FROZEN_EXISTING_FILE" "HEAD_LOC"
    for index in "${!FIXED_FILES[@]}"; do
        printf '%-104s %8d\n' "${FIXED_FILES[$index]}" "${BASELINE_FIXED_LOCS[$index]}"
        frozen_total=$((frozen_total + BASELINE_FIXED_LOCS[$index]))
    done
    if [[ "$frozen_total" -ne "$BASELINE_EXISTING_PRODUCTION_LOC" ]]; then
        echo "FAIL: 冻结基线内部求和错误 actual=$frozen_total expected=$BASELINE_EXISTING_PRODUCTION_LOC" >&2
        echo "architecture_check=FAILED failures=1"
        exit 1
    fi
    echo "baseline_existing_file_count=${#FIXED_FILES[@]}"
    echo "baseline_existing_production_loc=$BASELINE_EXISTING_PRODUCTION_LOC"
    echo "baseline_new_architecture_loc=0"
    echo "baseline_total_production_loc=$BASELINE_EXISTING_PRODUCTION_LOC"
    echo "architecture_check=PASSED failures=0"
    exit 0
fi

NEW_ARCHITECTURE_FILES=()
NEW_COMMAND_COORDINATOR_FILES=()

while IFS= read -r file; do
    [[ -n "$file" ]] || continue
    NEW_ARCHITECTURE_FILES+=("$file")
    case "$file" in
        */service/command/*|*/manager/*SecurityMutation*.java|*/service/impl/*SecurityMutationCoordinator.java|*CommandHandler.java)
            NEW_COMMAND_COORDINATOR_FILES+=("$file")
            ;;
    esac
done < <(
    find dealer-server/src/main/java/com/autodealer/crm -type f -name '*.java' -print \
        | awk '
            /\/service\/command\// ||
            /\/service\/policy\/(AuthorizationDelegationPolicy|AvailableAdministratorPolicy|ReportingGraphPolicy|AccountEmploymentStatePolicy|CredentialEligibilityPolicy)\.java$/ ||
            /\/event\/usermanagement\// ||
            /\/config\/aop\/UserManagement/ ||
            /\/manager\/.*SecurityMutation.*\.java$/ ||
            /\/service\/impl\/.*SecurityMutationCoordinator\.java$/ ||
            /CommandHandler\.java$/
        ' \
        | sort
)

require_files() {
    local file
    for file in "$@"; do
        if [[ ! -f "$file" ]]; then
            echo "ERROR: 固定统计文件不存在：$file" >&2
            exit 2
        fi
    done
}

line_count() {
    wc -l < "$1" | tr -d ' '
}

sum_lines() {
    local total=0
    local file
    for file in "$@"; do
        total=$((total + $(line_count "$file")))
    done
    echo "$total"
}

count_regex() {
    local expression="$1"
    shift
    if [[ $# -eq 0 ]]; then
        echo 0
        return
    fi
    { rg -o "$expression" "$@" 2>/dev/null || true; } | wc -l | tr -d ' '
}

count_fixed_text() {
    local expression="$1"
    shift
    if [[ $# -eq 0 ]]; then
        echo 0
        return
    fi
    { rg -F -o "$expression" "$@" 2>/dev/null || true; } | wc -l | tr -d ' '
}

constructor_metrics() {
    local file="$1"
    local class_name
    class_name="$(basename "$file" .java)"
    perl - "$file" "$class_name" <<'PERL'
use strict;
use warnings;

my ($file, $class_name) = @ARGV;
open my $fh, '<', $file or die "cannot open $file: $!";
local $/;
my $source = <$fh>;

if ($source !~ /\bpublic\s+\Q$class_name\E\s*\((.*?)\)\s*\{/s) {
    print "0 0 0";
    exit 0;
}

my $parameters = $1;
my @parts;
my $buffer = '';
my ($parentheses, $angles, $brackets, $braces) = (0, 0, 0, 0);

for my $char (split //, $parameters) {
    if ($char eq ',' && !$parentheses && !$angles && !$brackets && !$braces) {
        push @parts, $buffer;
        $buffer = '';
        next;
    }
    $buffer .= $char;
    $parentheses++ if $char eq '(';
    $parentheses-- if $char eq ')' && $parentheses;
    $angles++ if $char eq '<';
    $angles-- if $char eq '>' && $angles;
    $brackets++ if $char eq '[';
    $brackets-- if $char eq ']' && $brackets;
    $braces++ if $char eq '{';
    $braces-- if $char eq '}' && $braces;
}
push @parts, $buffer if $buffer =~ /\S/;

my $total = scalar @parts;
my $configuration = scalar grep { /\@Value\b/ } @parts;
my $collaborators = $total - $configuration;
print "$total $collaborators $configuration";
PERL
}

semicolon_tokens() {
    if [[ $# -eq 0 ]]; then
        echo 0
        return
    fi
    awk '{ line=$0; total+=gsub(/;/, ";", line) } END { print total+0 }' "$@"
}

compact_line_metrics() {
    if [[ $# -eq 0 ]]; then
        echo "0 0"
        return
    fi
    awk '
        length($0) > 180 { long_lines++ }
        {
            line=$0
            semicolons=gsub(/;/, ";", line)
            if (semicolons >= 4) dense_lines++
        }
        END { print long_lines+0, dense_lines+0 }
    ' "$@"
}

require_files "${FIXED_FILES[@]}"

FIXED_LOC_TOTAL="$(sum_lines "${FIXED_FILES[@]}")"
NEW_COMMAND_COORDINATOR_LOC="$(sum_lines "${NEW_COMMAND_COORDINATOR_FILES[@]}")"
NEW_ARCHITECTURE_LOC="$(sum_lines "${NEW_ARCHITECTURE_FILES[@]}")"
TRACKED_PRODUCTION_LOC=$((FIXED_LOC_TOTAL + NEW_ARCHITECTURE_LOC))

FIXED_DIRECT_REVOKE="$(count_regex '\.revokeAllForSecurityChange[[:space:]]*\(' "${FIXED_FILES[@]}")"
FIXED_DIRECT_INVALIDATE="$(count_regex '\.invalidateAfterCommit[[:space:]]*\(' "${FIXED_FILES[@]}")"
FIXED_DIRECT_REGISTER_SYNCHRONIZATION="$(count_regex 'TransactionSynchronizationManager[[:space:]]*\.[[:space:]]*registerSynchronization[[:space:]]*\(' "${FIXED_FILES[@]}")"
NEW_DIRECT_REVOKE="$(count_regex '\.revokeAllForSecurityChange[[:space:]]*\(' "${NEW_ARCHITECTURE_FILES[@]}")"
NEW_DIRECT_INVALIDATE="$(count_regex '\.invalidateAfterCommit[[:space:]]*\(' "${NEW_ARCHITECTURE_FILES[@]}")"
NEW_DIRECT_REGISTER_SYNCHRONIZATION="$(count_regex 'TransactionSynchronizationManager[[:space:]]*\.[[:space:]]*registerSynchronization[[:space:]]*\(' "${NEW_ARCHITECTURE_FILES[@]}")"
TRACKED_DIRECT_REVOKE=$((FIXED_DIRECT_REVOKE + NEW_DIRECT_REVOKE))
TRACKED_DIRECT_INVALIDATE=$((FIXED_DIRECT_INVALIDATE + NEW_DIRECT_INVALIDATE))
TRACKED_DIRECT_REGISTER_SYNCHRONIZATION=$((FIXED_DIRECT_REGISTER_SYNCHRONIZATION + NEW_DIRECT_REGISTER_SYNCHRONIZATION))
COORDINATOR_FILE="dealer-server/src/main/java/com/autodealer/crm/service/impl/UserSecurityMutationCoordinator.java"
ALLOWED_DIRECT_REVOKE="$(count_regex 'userSessionService[[:space:]]*\.[[:space:]]*revokeAllForSecurityChange[[:space:]]*\(' "$COORDINATOR_FILE")"
ALLOWED_DIRECT_INVALIDATE="$(count_regex 'ownerCandidateCacheInvalidator[[:space:]]*\.[[:space:]]*invalidateAfterCommit[[:space:]]*\(' "$COORDINATOR_FILE")"
FIXED_LOCK_TEXT=$((
    $(count_fixed_text 'lockGraph(' "${FIXED_FILES[@]}")
    + $(count_fixed_text 'lockMembership(' "${FIXED_FILES[@]}")
))

FIXED_FIELD_COLLABORATOR_INJECTION="$(count_regex '@(Resource|Autowired|Inject)\b' "${FIXED_FILES[@]}")"
FIXED_ALL_VALUE_INJECTION="$(count_regex '@Value\b' "${FIXED_FILES[@]}")"
NEW_FIELD_COLLABORATOR_INJECTION="$(count_regex '@(Resource|Autowired|Inject)\b' "${NEW_ARCHITECTURE_FILES[@]}")"
NEW_ALL_VALUE_INJECTION="$(count_regex '@Value\b' "${NEW_ARCHITECTURE_FILES[@]}")"

FIXED_CONSTRUCTOR_PARAMETERS=0
FIXED_CONSTRUCTOR_COLLABORATORS=0
FIXED_CONSTRUCTOR_CONFIG=0
FIXED_MAX_CONSTRUCTOR_COLLABORATORS=0
FIXED_MAX_FILE_LOC=0
NEW_MAX_CONSTRUCTOR_COLLABORATORS=0
NEW_MAX_FILE_LOC=0
FIXED_FILE_LOCS=()
FIXED_FILE_CONSTRUCTOR_COLLABORATORS=()

echo "mode=$MODE"
echo "line_count_scope=physical_lines_including_blank_and_comment_lines"
echo "direct_call_scope=receiver_dot_invocations_method_declarations_excluded"

printf '%-104s %8s %8s %8s %8s\n' "FILE" "LOC" "CTOR" "COLLAB" "CONFIG"
for file in "${FIXED_FILES[@]}"; do
    loc="$(line_count "$file")"
    read -r constructor_parameters constructor_collaborators constructor_config <<< "$(constructor_metrics "$file")"
    FIXED_FILE_LOCS+=("$loc")
    FIXED_FILE_CONSTRUCTOR_COLLABORATORS+=("$constructor_collaborators")
    FIXED_CONSTRUCTOR_PARAMETERS=$((FIXED_CONSTRUCTOR_PARAMETERS + constructor_parameters))
    FIXED_CONSTRUCTOR_COLLABORATORS=$((FIXED_CONSTRUCTOR_COLLABORATORS + constructor_collaborators))
    FIXED_CONSTRUCTOR_CONFIG=$((FIXED_CONSTRUCTOR_CONFIG + constructor_config))
    (( constructor_collaborators > FIXED_MAX_CONSTRUCTOR_COLLABORATORS )) && FIXED_MAX_CONSTRUCTOR_COLLABORATORS=$constructor_collaborators
    (( loc > FIXED_MAX_FILE_LOC )) && FIXED_MAX_FILE_LOC=$loc
    printf '%-104s %8d %8d %8d %8d\n' "$file" "$loc" "$constructor_parameters" "$constructor_collaborators" "$constructor_config"
done

if [[ ${#NEW_ARCHITECTURE_FILES[@]} -gt 0 ]]; then
    for file in "${NEW_ARCHITECTURE_FILES[@]}"; do
        loc="$(line_count "$file")"
        read -r constructor_parameters constructor_collaborators constructor_config <<< "$(constructor_metrics "$file")"
        (( constructor_collaborators > NEW_MAX_CONSTRUCTOR_COLLABORATORS )) && NEW_MAX_CONSTRUCTOR_COLLABORATORS=$constructor_collaborators
        (( loc > NEW_MAX_FILE_LOC )) && NEW_MAX_FILE_LOC=$loc
        printf '%-104s %8d %8d %8d %8d\n' "$file" "$loc" "$constructor_parameters" "$constructor_collaborators" "$constructor_config"
    done
fi

FIXED_FIELD_CONFIG_INJECTION=$((FIXED_ALL_VALUE_INJECTION - FIXED_CONSTRUCTOR_CONFIG))
NEW_CONSTRUCTOR_CONFIG=0
if [[ ${#NEW_ARCHITECTURE_FILES[@]} -gt 0 ]]; then
    for file in "${NEW_ARCHITECTURE_FILES[@]}"; do
        read -r _ _ constructor_config <<< "$(constructor_metrics "$file")"
        NEW_CONSTRUCTOR_CONFIG=$((NEW_CONSTRUCTOR_CONFIG + constructor_config))
    done
fi
NEW_FIELD_CONFIG_INJECTION=$((NEW_ALL_VALUE_INJECTION - NEW_CONSTRUCTOR_CONFIG))

FIXED_SEMICOLON_TOKENS="$(semicolon_tokens "${FIXED_FILES[@]}")"
read -r TRACKED_LONG_LINES TRACKED_DENSE_LINES <<< "$(compact_line_metrics "${FIXED_FILES[@]}" "${NEW_ARCHITECTURE_FILES[@]}")"

echo "fixed_file_count=${#FIXED_FILES[@]}"
echo "fixed_loc_total=$FIXED_LOC_TOTAL"
echo "existing_file_count=${#FIXED_FILES[@]}"
echo "existing_production_loc=$FIXED_LOC_TOTAL"
echo "new_command_coordinator_file_count=${#NEW_COMMAND_COORDINATOR_FILES[@]}"
echo "new_command_coordinator_loc=$NEW_COMMAND_COORDINATOR_LOC"
echo "new_architecture_file_count=${#NEW_ARCHITECTURE_FILES[@]}"
echo "new_architecture_loc=$NEW_ARCHITECTURE_LOC"
echo "tracked_production_loc=$TRACKED_PRODUCTION_LOC"
echo "frozen_baseline_production_loc=$BASELINE_EXISTING_PRODUCTION_LOC"
echo "net_production_loc_change=$((TRACKED_PRODUCTION_LOC - BASELINE_EXISTING_PRODUCTION_LOC))"
echo "fixed_constructor_parameters=$FIXED_CONSTRUCTOR_PARAMETERS"
echo "fixed_constructor_collaborators=$FIXED_CONSTRUCTOR_COLLABORATORS"
echo "fixed_constructor_config_parameters=$FIXED_CONSTRUCTOR_CONFIG"
echo "fixed_max_constructor_collaborators=$FIXED_MAX_CONSTRUCTOR_COLLABORATORS"
echo "new_max_constructor_collaborators=$NEW_MAX_CONSTRUCTOR_COLLABORATORS"
echo "fixed_field_collaborator_injection=$FIXED_FIELD_COLLABORATOR_INJECTION"
echo "fixed_field_config_injection=$FIXED_FIELD_CONFIG_INJECTION"
echo "new_field_collaborator_injection=$NEW_FIELD_COLLABORATOR_INJECTION"
echo "new_field_config_injection=$NEW_FIELD_CONFIG_INJECTION"
echo "fixed_direct_revoke_calls=$FIXED_DIRECT_REVOKE"
echo "fixed_direct_invalidate_calls=$FIXED_DIRECT_INVALIDATE"
echo "fixed_direct_register_synchronization_calls=$FIXED_DIRECT_REGISTER_SYNCHRONIZATION"
echo "new_direct_revoke_calls=$NEW_DIRECT_REVOKE"
echo "new_direct_invalidate_calls=$NEW_DIRECT_INVALIDATE"
echo "new_direct_register_synchronization_calls=$NEW_DIRECT_REGISTER_SYNCHRONIZATION"
echo "tracked_direct_revoke_calls=$TRACKED_DIRECT_REVOKE"
echo "tracked_direct_invalidate_calls=$TRACKED_DIRECT_INVALIDATE"
echo "tracked_direct_register_synchronization_calls=$TRACKED_DIRECT_REGISTER_SYNCHRONIZATION"
echo "allowed_coordinator_direct_revoke_calls=$ALLOWED_DIRECT_REVOKE"
echo "allowed_coordinator_direct_invalidate_calls=$ALLOWED_DIRECT_INVALIDATE"
echo "fixed_lock_text_occurrences=$FIXED_LOCK_TEXT"
echo "fixed_semicolon_tokens=$FIXED_SEMICOLON_TOKENS"
echo "tracked_long_lines_over_180=$TRACKED_LONG_LINES"
echo "tracked_semicolon_dense_lines=$TRACKED_DENSE_LINES"

FAILURES=0

check_equal() {
    local label="$1"
    local actual="$2"
    local expected="$3"
    if [[ "$actual" -ne "$expected" ]]; then
        echo "FAIL: $label actual=$actual expected=$expected" >&2
        FAILURES=$((FAILURES + 1))
    fi
}

check_strictly_less_than() {
    local label="$1"
    local actual="$2"
    local ceiling="$3"
    if [[ "$actual" -ge "$ceiling" ]]; then
        echo "FAIL: $label actual=$actual must_be_strictly_less_than=$ceiling" >&2
        FAILURES=$((FAILURES + 1))
    fi
}

check_strictly_less_than "tracked_production_loc" "$TRACKED_PRODUCTION_LOC" "$BASELINE_EXISTING_PRODUCTION_LOC"

# 访问变化后的会话撤销只允许由协调器调用 UserSessionServiceImpl。
check_equal "tracked_direct_revoke_calls" "$TRACKED_DIRECT_REVOKE" "$ALLOWED_DIRECT_REVOKE"
check_equal "allowed_coordinator_direct_revoke_calls" "$ALLOWED_DIRECT_REVOKE" 1

# 负责人候选缓存失效只允许由协调器调用统一 invalidator；协调器内部可以有多个明确入口。
check_equal "tracked_direct_invalidate_calls" "$TRACKED_DIRECT_INVALIDATE" "$ALLOWED_DIRECT_INVALIDATE"
if [[ "$ALLOWED_DIRECT_INVALIDATE" -lt 1 ]]; then
    echo "FAIL: 协调器缺少 OwnerCandidateCacheInvalidator 调用" >&2
    FAILURES=$((FAILURES + 1))
fi

# UserSessionServiceImpl 内的专用事务同步仅报告，不为通过门禁强制引入额外事件框架。

if [[ "$FAILURES" -ne 0 ]]; then
    echo "architecture_check=FAILED failures=$FAILURES"
    exit 1
fi

echo "architecture_check=PASSED failures=0"
