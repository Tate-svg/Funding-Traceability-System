-- 先在MySQL客户端选择现有业务库，再执行本文件；不选择隐私库。
-- 增量迁移：不删除数据，不推测旧评分的历史权重或资助单；可重复执行。
-- 上线顺序：备份业务库 -> 执行本文件 -> 更新后端与前端 -> 重启后端加载权限。

SET @migration_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='score' AND column_name='fund_id')=0, 'ALTER TABLE `score` ADD COLUMN `fund_id` BIGINT NULL COMMENT ''关联资助单；历史不自动回填''', 'SELECT 1');
PREPARE funding_migration FROM @migration_sql;
EXECUTE funding_migration;
DEALLOCATE PREPARE funding_migration;

SET @migration_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='score' AND column_name='rule_version')=0, 'ALTER TABLE `score` ADD COLUMN `rule_version` VARCHAR(64) NULL COMMENT ''历史规则未留存时为NULL''', 'SELECT 1');
PREPARE funding_migration FROM @migration_sql;
EXECUTE funding_migration;
DEALLOCATE PREPARE funding_migration;

SET @migration_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='score' AND column_name='rule_captured_at')=0, 'ALTER TABLE `score` ADD COLUMN `rule_captured_at` DATETIME NULL', 'SELECT 1');
PREPARE funding_migration FROM @migration_sql;
EXECUTE funding_migration;
DEALLOCATE PREPARE funding_migration;

SET @migration_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='score_detail' AND column_name='item_name_snapshot')=0, 'ALTER TABLE `score_detail` ADD COLUMN `item_name_snapshot` VARCHAR(50) NULL', 'SELECT 1');
PREPARE funding_migration FROM @migration_sql;
EXECUTE funding_migration;
DEALLOCATE PREPARE funding_migration;

SET @migration_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='score_detail' AND column_name='weight_snapshot')=0, 'ALTER TABLE `score_detail` ADD COLUMN `weight_snapshot` DECIMAL(5,2) NULL', 'SELECT 1');
PREPARE funding_migration FROM @migration_sql;
EXECUTE funding_migration;
DEALLOCATE PREPARE funding_migration;

SET @migration_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='score' AND index_name='idx_score_fund')=0, 'ALTER TABLE score ADD INDEX idx_score_fund (fund_id)', 'SELECT 1');
PREPARE funding_migration FROM @migration_sql;
EXECUTE funding_migration;
DEALLOCATE PREPARE funding_migration;
INSERT INTO role_permission (role,permission) SELECT 'AUDITOR','audit:readonly' WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role='AUDITOR' AND permission='audit:readonly');
-- 不创建固定密码账号：请由审核管理员在用户管理中创建只读审计账号。
