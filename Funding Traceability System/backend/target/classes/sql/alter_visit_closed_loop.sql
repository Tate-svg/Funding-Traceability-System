-- 已有数据库升级脚本：回访闭环功能（资助-拨付-回访）
-- 适用：已经执行过旧版 schema.sql、且库中已有数据的 funding_db
-- 执行方式：mysql -u root -p funding_db < alter_visit_closed_loop.sql
-- 说明：仅新增列和索引，不删除、不修改任何现有列与数据

USE funding_db;

-- 绑定的资助流水号（fund.id），可空，兼容历史回访记录
ALTER TABLE visit
    ADD COLUMN fund_id BIGINT NULL COMMENT '绑定的资助流水号（fund.id）' AFTER sponsor_id;

-- 脱敏回访记录：content/situation/problem 自动脱敏合并生成
ALTER TABLE visit
    ADD COLUMN masked_record TEXT NULL COMMENT '脱敏回访记录' AFTER visitor;

-- 脱敏影像/凭证材料：上传时自动脱敏后的存储文件名
ALTER TABLE visit
    ADD COLUMN masked_material VARCHAR(255) NULL COMMENT '脱敏凭证材料存储文件名' AFTER masked_record;

-- 按资助流水反查回访
ALTER TABLE visit
    ADD INDEX idx_visit_fund (fund_id);
