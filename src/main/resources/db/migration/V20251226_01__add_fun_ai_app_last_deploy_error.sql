-- 为 fun_ai_app 增加 last_deploy_error 字段（可重复执行）
-- 注意：本项目当前未引入 Flyway/Liquibase，若未启用迁移框架，请手动执行本 SQL。

SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fun_ai_app'
      AND COLUMN_NAME = 'last_deploy_error'
);

SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE fun_ai_app ADD COLUMN last_deploy_error VARCHAR(2000) NULL COMMENT ''最近一次部署失败原因'';',
    'SELECT 1;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


