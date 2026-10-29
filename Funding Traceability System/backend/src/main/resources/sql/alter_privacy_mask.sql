-- 已有数据库升级脚本：敏感字段二次遮挡 + 限时查看会话
-- 适用：已经执行过旧版 schema.sql、且库中已有数据的 funding_db
-- 执行方式：mysql -u root -p funding_db < alter_privacy_mask.sql
-- 说明：仅新增表，不修改任何现有表与数据；脚本可重复执行（IF NOT EXISTS）

USE funding_db;

-- 管理员敏感字段二次遮挡开关（按管理员独立保存）
CREATE TABLE IF NOT EXISTS admin_privacy_config (
    id                  BIGINT   NOT NULL AUTO_INCREMENT,
    admin_id            BIGINT   NOT NULL COMMENT '管理员用户id（sys_user.id）',
    hide_family_address TINYINT  NOT NULL DEFAULT 1 COMMENT '二次遮挡家庭住址：1开 0关',
    hide_bank_card      TINYINT  NOT NULL DEFAULT 1 COMMENT '二次遮挡银行卡：1开 0关',
    create_time         DATETIME NULL,
    update_time         DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin (admin_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员敏感字段二次遮挡配置';

-- 敏感详情限时查看会话（到期或手动关闭后失效）
CREATE TABLE IF NOT EXISTS sensitive_view_session (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    session_id        VARCHAR(64) NOT NULL COMMENT '会话凭证（UUID）',
    admin_id          BIGINT      NULL COMMENT '发起查看的管理员id',
    target_profile_id BIGINT      NULL COMMENT '查看对象：privacy_access_apply.id',
    start_time        DATETIME    NULL,
    expire_time       DATETIME    NULL COMMENT '会话到期时间（默认15分钟）',
    is_valid          TINYINT     DEFAULT 1 COMMENT '1有效 0已关闭/失效',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session (session_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '敏感详情限时查看会话';
