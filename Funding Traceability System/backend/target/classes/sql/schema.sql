-- 助学帮扶管理系统 数据库初始化脚本
-- 使用前请确保MySQL已启动且拥有创建数据库权限

DROP DATABASE IF EXISTS funding_db;
CREATE DATABASE funding_db DEFAULT CHARACTER SET utf8mb4;

-- 隐私库：独立存储学生敏感信息（AES 加密），与业务库隔离，通过 virtual_id 映射
DROP DATABASE IF EXISTS funding_privacy;
CREATE DATABASE funding_privacy DEFAULT CHARACTER SET utf8mb4;

USE funding_db;

DROP TABLE IF EXISTS score_detail;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS score_item;
DROP TABLE IF EXISTS student_attachment;
DROP TABLE IF EXISTS visit;
DROP TABLE IF EXISTS mask_violation_log;
DROP TABLE IF EXISTS privacy_access_log;
DROP TABLE IF EXISTS privacy_access_apply;
DROP TABLE IF EXISTS timestamp_record;
DROP TABLE IF EXISTS review_task;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS fund;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学生表（业务库）：只存虚拟ID + 脱敏名 + 非敏感字段，隐私明文见 funding_privacy.privacy_profile
CREATE TABLE student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    virtual_id VARCHAR(20) NOT NULL UNIQUE,
    mask_name VARCHAR(20),
    gender VARCHAR(10),
    school VARCHAR(100),
    grade VARCHAR(50),
    sponsor_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    sponsor_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    fund_date DATE,
    purpose VARCHAR(255),
    remark VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    voucher_no VARCHAR(50) UNIQUE,
    voucher_hash VARCHAR(64),
    audit_by BIGINT,
    audit_time DATETIME,
    audit_remark VARCHAR(255),
    disburse_by BIGINT,
    disburse_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fund_status (status),
    INDEX idx_fund_amount (amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE visit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    sponsor_id BIGINT NOT NULL,
    -- 绑定的资助流水号（fund.id），形成 资助-拨付-回访 闭环
    fund_id BIGINT,
    visit_date DATE,
    content TEXT,
    situation VARCHAR(255),
    problem TEXT,
    visitor VARCHAR(50),
    -- 脱敏回访记录：由 content/situation/problem 自动脱敏合并生成（只保留核验必要信息）
    masked_record TEXT,
    -- 脱敏影像/凭证材料：上传时自动脱敏（文本内容脱敏/图片剥离EXIF元数据）后的存储文件名
    masked_material VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_visit_fund (fund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE score_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学生证明材料（上传后经脱敏预览确认入库）
CREATE TABLE student_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    file_name VARCHAR(100),
    file_path VARCHAR(255),
    mask_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    masked_text TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_att_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    sponsor_id BIGINT NOT NULL,
    score_date DATE,
    total_score DECIMAL(6,2),
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE score_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    score_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_score DECIMAL(5,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色-权限点映射表
CREATE TABLE role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role VARCHAR(20) NOT NULL,
    permission VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_role_perm (role, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志（只读存证，无 update/delete 接口）
CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    role VARCHAR(20),
    operation VARCHAR(100),
    target_type VARCHAR(50),
    target_id BIGINT,
    content VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_time (create_time),
    INDEX idx_log_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 大额复核抽检任务
CREATE TABLE review_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_id BIGINT NOT NULL,
    batch_no VARCHAR(30),
    sample_ratio DECIMAL(5,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    review_remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    review_time DATETIME,
    INDEX idx_review_fund (fund_id),
    INDEX idx_review_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 隐私明文调取审批单（两级审批：审核管理员初审 -> 监事终审，通过限时可见）
CREATE TABLE privacy_access_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_id BIGINT,
    applicant_name VARCHAR(50),
    virtual_id VARCHAR(20) NOT NULL,
    field_scope VARCHAR(100),
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_L1',
    l1_by BIGINT,
    l1_time DATETIME,
    l1_remark VARCHAR(255),
    l2_by BIGINT,
    l2_time DATETIME,
    l2_remark VARCHAR(255),
    expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_apply_virtual (virtual_id),
    INDEX idx_apply_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 隐私明文调取留痕（只读存证，无 update/delete 接口）
CREATE TABLE privacy_access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_id BIGINT,
    user_id BIGINT,
    username VARCHAR(50),
    virtual_id VARCHAR(20),
    fields VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pal_virtual (virtual_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 脱敏拦截记录（只读存证：未脱敏内容被拦截即记录）
CREATE TABLE mask_violation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    api VARCHAR(100),
    field VARCHAR(50),
    rule VARCHAR(50),
    hit_text VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可信时间戳台账（本地模拟 TSA：全局序列号 + SHA-256 哈希链固化时序，只读存证）
CREATE TABLE timestamp_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    serial_no BIGINT NOT NULL UNIQUE,
    biz_type VARCHAR(50) NOT NULL,
    biz_id BIGINT,
    node VARCHAR(30) NOT NULL,
    node_name VARCHAR(50),
    summary VARCHAR(200),
    operator_id BIGINT,
    operator_name VARCHAR(50),
    role VARCHAR(20),
    ts_time DATETIME,
    ts_hash VARCHAR(64) NOT NULL,
    prev_hash VARCHAR(64),
    anomaly TINYINT DEFAULT 0,
    anomaly_reason VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ts_biz (biz_type, biz_id),
    INDEX idx_ts_serial (serial_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 隐私库 funding_privacy ====================
-- 学生隐私档案：五个敏感字段 AES 加密存储，通过 virtual_id 与业务库 student 一一映射
USE funding_privacy;

CREATE TABLE privacy_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    virtual_id VARCHAR(20) NOT NULL UNIQUE,
    name_enc VARCHAR(255),
    id_card_enc VARCHAR(255),
    phone_enc VARCHAR(255),
    family_address_enc VARCHAR(512),
    family_situation_enc TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE funding_db;

-- 预置用户数据（Demo简化：密码明文存储）
-- 角色：AUDIT_ADMIN 审核管理员 / DISBURSE_ADMIN 拨付管理员 / SUPERVISOR 监事 / SPONSOR 资助人
INSERT INTO sys_user (id, username, password, real_name, role, phone) VALUES
(1, 'audit_admin', '123456', '王审核', 'AUDIT_ADMIN', '13800000001'),
(2, 'sponsor', '123456', '张资助', 'SPONSOR', '13800000002'),
(3, 'disburse_admin', '123456', '李拨付', 'DISBURSE_ADMIN', '13800000003'),
(4, 'supervisor', '123456', '赵监事', 'SUPERVISOR', '13800000004'),
-- 第二审批人：满足「申请人不能审批本人申请」的双人审批约束
(5, 'audit_admin2', '123456', '李复审', 'AUDIT_ADMIN', '13800000005'),
(6, 'supervisor2', '123456', '孙监事', 'SUPERVISOR', '13800000006');

-- 角色权限点预置
INSERT INTO role_permission (role, permission) VALUES
-- 审核管理员：申请核验 + 凭证生成 + 基础数据管理，无拨付权限
('AUDIT_ADMIN', 'fund:view'),
('AUDIT_ADMIN', 'fund:create'),
('AUDIT_ADMIN', 'fund:update'),
('AUDIT_ADMIN', 'fund:delete'),
('AUDIT_ADMIN', 'fund:audit'),
('AUDIT_ADMIN', 'fund:export'),
('AUDIT_ADMIN', 'student:manage'),
('AUDIT_ADMIN', 'visit:manage'),
('AUDIT_ADMIN', 'score:manage'),
('AUDIT_ADMIN', 'scoreItem:manage'),
('AUDIT_ADMIN', 'user:manage'),
('AUDIT_ADMIN', 'log:view'),
('AUDIT_ADMIN', 'privacy:apply'),
('AUDIT_ADMIN', 'privacy:approve:l1'),
-- 审核管理员：档案核验职责所需，学生隐私明文可直接查看（全程留痕，无需审批）
('AUDIT_ADMIN', 'privacy:view:direct'),
-- 拨付管理员：仅查看已审单据 + 执行发放（不可查看敏感隐私，无 privacy:apply）
('DISBURSE_ADMIN', 'fund:view'),
('DISBURSE_ADMIN', 'fund:disburse'),
-- 监事/审计：大额抽检复核 + 日志查看 + 隐私调取终审
('SUPERVISOR', 'fund:view'),
('SUPERVISOR', 'review:draw'),
('SUPERVISOR', 'review:finish'),
('SUPERVISOR', 'log:view'),
('SUPERVISOR', 'privacy:apply'),
('SUPERVISOR', 'privacy:approve:l2'),
-- 资助人：仅查看
('SPONSOR', 'fund:view'),
('SPONSOR', 'privacy:apply');

-- 评分指标（权重总和100）
INSERT INTO score_item (id, name, weight, description) VALUES
(1, '学习情况', 30.00, '学习成绩与学习态度'),
(2, '生活改善', 25.00, '日常生活条件改善情况'),
(3, '心理状态', 20.00, '心理健康与情绪状态'),
(4, '家庭帮扶成效', 25.00, '家庭整体帮扶成效');

-- 学生数据（资助人 sponsor_id=2；create_time 早于资金流转时间，保证时间戳时序自洽）
-- 业务库只存虚拟ID + 脱敏名 + 非敏感字段
INSERT INTO student (id, virtual_id, mask_name, gender, school, grade, sponsor_id, create_time) VALUES
(1, 'STU-a1b2c3d4e5f6', '李**', '男', '北京第一中学', '高二', 2, '2024-02-20 09:00:00'),
(2, 'STU-b2c3d4e5f6a7', '王**', '女', '北京第二中学', '高一', 2, '2024-02-20 09:05:00'),
(3, 'STU-c3d4e5f6a7b8', '陈**', '男', '北京第三中学', '高三', 2, '2024-02-20 09:10:00');

-- 学生隐私明文（AES/ECB/PKCS5Padding Base64 密文，密钥见 application.yml funding.privacy.aes-key）
-- 与业务库 student.virtual_id 一一映射，明文依次为：姓名/身份证/电话/家庭住址/家庭情况
INSERT INTO funding_privacy.privacy_profile (id, virtual_id, name_enc, id_card_enc, phone_enc, family_address_enc, family_situation_enc, create_time) VALUES
(1, 'STU-a1b2c3d4e5f6', 'MNYO4hrKBRzij0hwSTZwZw==', 'tP03usypWIpXpi7CTuGUGGhYTEc0c8Jry+1iq1czVPA=', 'inTf4RpgHKEgLjEkpvLJxg==', 'E7+nmyLOyTyU09O9vKwyYqiZ2G2KJwmKCpUbInw8ySvPjc+j/omJrETAUGcDkuPM', 'Dp2vN4p9uBXiwhDAtKINs/nFGtttB2Gtr2qs2BIGpkIfefI5BUUPspcYn/uMckNtb5GgSLMpQdBeJNl0r+OBY17rmCnDdF50nHyffu+YHgk=', '2024-02-20 09:00:00'),
(2, 'STU-b2c3d4e5f6a7', 'I7SOnS0W34KPK1UAPRsW7A==', 'o7N0K6o9FaC2z9WNPrI7r3PVYq9YKJbb16X+/5F71js=', 'XmAbPSeyRFvdqRG99TOflQ==', '2PYCxJZIsYuGDTiW4yyVq9hPjkhHRDdergOgQ42x5IM=', 'm6BWCwar2AB7JL7NBXFGf2NPI1jyvVpW9VI49XumN98tca0S1BVqH+pKWXsUI7Ca', '2024-02-20 09:05:00'),
(3, 'STU-c3d4e5f6a7b8', 'di0Lmb87lnKbadgxxKwycg==', 'nBmeR0NTQK0P1WW49cBsGGDTH9bSMhIfO/EP0gDFfoE=', 'zyBEFJn97KJ9Ke8GcwNz7w==', 'XIzvPVplMSRVz4JUmn8n9xngiIbGQisXxAfBXpQExH7vNY0vAZog3Dj3S8jfjBTk', 'eAyN4OqG0KxJ00GLFGOn5FvrJyNC+x5eQtVdkm7dPi3Z0rmUVL2stVxLeIu0ArXVfR643kyrfX4AeXPEFwZCOQ==', '2024-02-20 09:10:00');

-- 资金流水（存量5条标记为已拨付；另预置1条6000元已审核大额单用于抽检演示，1条3000元已审核小额单用于直接拨付演示）
-- create_time 显式指定且早于各自审核时间，保证时间戳业务时序自洽
INSERT INTO fund (id, student_id, sponsor_id, amount, fund_date, purpose, remark, status, voucher_no, voucher_hash, audit_by, audit_time, audit_remark, disburse_by, disburse_time, create_time) VALUES
(1, 1, 2, 2000.00, '2024-03-01', '学费', '春季学期学费', 'DISBURSED', 'Voucher-20240301-0001', NULL, 1, '2024-03-01 09:00:00', '资料齐全，予以通过', 3, '2024-03-02 10:00:00', '2024-02-28 16:00:00'),
(2, 1, 2, 1500.00, '2024-05-10', '生活费', '五月生活费补助', 'DISBURSED', 'Voucher-20240510-0001', NULL, 1, '2024-05-10 09:00:00', '情况属实', 3, '2024-05-11 10:00:00', '2024-05-09 15:00:00'),
(3, 2, 2, 1800.00, '2024-03-05', '学费', '春季学期学费', 'DISBURSED', 'Voucher-20240305-0001', NULL, 1, '2024-03-05 09:00:00', '情况属实', 3, '2024-03-06 10:00:00', '2024-03-04 15:00:00'),
(4, 2, 2, 1200.00, '2024-06-01', '书本费', '购买教辅资料', 'DISBURSED', 'Voucher-20240601-0001', NULL, 1, '2024-06-01 09:00:00', '情况属实', 3, '2024-06-02 10:00:00', '2024-05-31 15:00:00'),
(5, 3, 2, 3000.00, '2024-03-10', '学费', '高考冲刺学期学费', 'DISBURSED', 'Voucher-20240310-0001', NULL, 1, '2024-03-10 09:00:00', '情况属实', 3, '2024-03-11 10:00:00', '2024-03-09 15:00:00'),
(6, 3, 2, 6000.00, '2026-10-15', '学费', '大学预科全年学费及住宿费', 'AUDITED', 'Voucher-20261015-0001', NULL, 1, '2026-10-15 14:30:00', '大额资助，资料核验通过，待复核', NULL, NULL, '2026-10-14 20:00:00'),
(7, 1, 2, 3000.00, '2026-10-16', '生活费', '冬季生活费及取暖补助', 'AUDITED', 'Voucher-20261016-0001', NULL, 1, '2026-10-16 10:00:00', '情况属实，同意资助', NULL, NULL, '2026-10-15 20:00:00');

-- 回访记录
INSERT INTO visit (id, student_id, sponsor_id, visit_date, content, situation, problem, visitor) VALUES
(1, 1, 2, '2024-04-15', '入户走访，了解学习和生活情况', '学习态度积极，成绩稳步提升', '家庭仍需持续帮扶', '张资助'),
(2, 2, 2, '2024-04-20', '电话回访及家访', '性格开朗，适应高中生活', '母亲身体欠佳需关注', '张资助'),
(3, 3, 2, '2024-04-25', '高考前心理疏导走访', '压力较大但状态稳定', '需加强心理辅导', '张资助');

-- 评分记录
-- 李明: 90/85/80/88 -> 90*0.3+85*0.25+80*0.2+88*0.25 = 27+21.25+16+22 = 86.25
INSERT INTO score (id, student_id, sponsor_id, score_date, total_score, remark) VALUES
(1, 1, 2, '2024-06-30', 86.25, '整体表现良好，学习稳步提升'),
-- 王红: 88/92/85/90 -> 88*0.3+92*0.25+85*0.2+90*0.25 = 26.4+23+17+22.5 = 88.90
(2, 2, 2, '2024-06-30', 88.90, '进步明显，生活改善显著'),
-- 陈强: 75/80/70/78 -> 75*0.3+80*0.25+70*0.2+78*0.25 = 22.5+20+14+19.5 = 76.00
(3, 3, 2, '2024-06-30', 76.00, '需继续努力，关注心理状态');

-- 评分明细
INSERT INTO score_detail (id, score_id, item_id, item_score) VALUES
(1, 1, 1, 90.00),
(2, 1, 2, 85.00),
(3, 1, 3, 80.00),
(4, 1, 4, 88.00),
(5, 2, 1, 88.00),
(6, 2, 2, 92.00),
(7, 2, 3, 85.00),
(8, 2, 4, 90.00),
(9, 3, 1, 75.00),
(10, 3, 2, 80.00),
(11, 3, 3, 70.00),
(12, 3, 4, 78.00);
