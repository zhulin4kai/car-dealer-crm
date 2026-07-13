-- Task 23：代理管理关系命令与用户安全审计动作长度闭环。

CALL crm_require_migration_context('20260713_task23_acting_reporting_and_audit_width');

DROP PROCEDURE IF EXISTS task23_prepare_acting_reporting;
DELIMITER $$
CREATE PROCEDURE task23_prepare_acting_reporting()
BEGIN
  CALL crm_require_migration_context('20260713_task23_acting_reporting_and_audit_width');

  ALTER TABLE t_operation_log
    MODIFY COLUMN action_code VARCHAR(64) NOT NULL COMMENT '审计动作代码';

  IF EXISTS (
    SELECT 1 FROM t_employee_reporting
    WHERE relation_type='ACTING' AND effective_to IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Task23要求所有代理管理关系具有有限失效时间';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_employee_reporting'
      AND CONSTRAINT_NAME='chk_employee_reporting_acting_finite'
  ) THEN
    ALTER TABLE t_employee_reporting
      ADD CONSTRAINT chk_employee_reporting_acting_finite
      CHECK (relation_type<>'ACTING' OR effective_to IS NOT NULL);
  END IF;
END$$
DELIMITER ;

CALL task23_prepare_acting_reporting();
DROP PROCEDURE task23_prepare_acting_reporting;
CALL crm_migration_mark_step('20260713_task23_acting_reporting_and_audit_width','ACTING_REPORTING_READY');
