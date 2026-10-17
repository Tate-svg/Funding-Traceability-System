-- 助学帮扶管理系统 数据库初始化脚本
-- 使用前请确保MySQL已启动且拥有创建数据库权限

DROP DATABASE IF EXISTS funding_db;
CREATE DATABASE funding_db DEFAULT CHARACTER SET utf8mb4;
USE funding_db;

DROP TABLE IF EXISTS score_detail;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS score_item;
DROP TABLE IF EXISTS visit;
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

CREATE TABLE student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    id_card VARCHAR(20),
    school VARCHAR(100),
    grade VARCHAR(50),
    family_address VARCHAR(255),
    family_situation TEXT,
    phone VARCHAR(20),
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
    visit_date DATE,
    content TEXT,
    situation VARCHAR(255),
    problem TEXT,
    visitor VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE score_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    description VARCHAR(255)
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

-- 预置用户数据（Demo简化：密码明文存储）
-- 角色：AUDIT_ADMIN 审核管理员 / DISBURSE_ADMIN 拨付管理员 / SUPERVISOR 监事 / SPONSOR 资助人
INSERT INTO sys_user (id, username, password, real_name, role, phone) VALUES
(1, 'audit_admin', '123456', '王审核', 'AUDIT_ADMIN', '13800000001'),
(2, 'sponsor', '123456', '张资助', 'SPONSOR', '13800000002'),
(3, 'disburse_admin', '123456', '李拨付', 'DISBURSE_ADMIN', '13800000003'),
(4, 'supervisor', '123456', '赵监事', 'SUPERVISOR', '13800000004');

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
-- 拨付管理员：仅查看已审单据 + 执行发放
('DISBURSE_ADMIN', 'fund:view'),
('DISBURSE_ADMIN', 'fund:disburse'),
-- 监事/审计：大额抽检复核 + 日志查看
('SUPERVISOR', 'fund:view'),
('SUPERVISOR', 'review:draw'),
('SUPERVISOR', 'review:finish'),
('SUPERVISOR', 'log:view'),
-- 资助人：仅查看
('SPONSOR', 'fund:view');

-- 评分指标（权重总和100）
INSERT INTO score_item (id, name, weight, description) VALUES
(1, '学习情况', 30.00, '学习成绩与学习态度'),
(2, '生活改善', 25.00, '日常生活条件改善情况'),
(3, '心理状态', 20.00, '心理健康与情绪状态'),
(4, '家庭帮扶成效', 25.00, '家庭整体帮扶成效');

-- 学生数据（资助人 sponsor_id=2）
INSERT INTO student (id, name, gender, id_card, school, grade, family_address, family_situation, phone, sponsor_id) VALUES
(1, '李明', '男', '110101201001010015', '北京第一中学', '高二', '北京市朝阳区幸福路10号', '父亲因病去世，母亲打零工收入微薄，家庭经济困难', '13800000011', 2),
(2, '王红', '女', '110101200901010028', '北京第二中学', '高一', '北京市海淀区希望路5号', '单亲家庭，母亲务农，收入不稳定', '13800000012', 2),
(3, '陈强', '男', '110101200801010037', '北京第三中学', '高三', '北京市西城区阳光胡同3号', '父母双残，无劳动能力，靠低保维持生活', '13800000013', 2);

-- 资金流水（存量5条标记为已拨付；另预置1条6000元已审核大额单用于抽检演示，1条3000元已审核小额单用于直接拨付演示）
INSERT INTO fund (id, student_id, sponsor_id, amount, fund_date, purpose, remark, status, voucher_no, voucher_hash, audit_by, audit_time, audit_remark, disburse_by, disburse_time) VALUES
(1, 1, 2, 2000.00, '2024-03-01', '学费', '春季学期学费', 'DISBURSED', 'Voucher-20240301-0001', NULL, 1, '2024-03-01 09:00:00', '资料齐全，予以通过', 3, '2024-03-02 10:00:00'),
(2, 1, 2, 1500.00, '2024-05-10', '生活费', '五月生活费补助', 'DISBURSED', 'Voucher-20240510-0001', NULL, 1, '2024-05-10 09:00:00', '情况属实', 3, '2024-05-11 10:00:00'),
(3, 2, 2, 1800.00, '2024-03-05', '学费', '春季学期学费', 'DISBURSED', 'Voucher-20240305-0001', NULL, 1, '2024-03-05 09:00:00', '情况属实', 3, '2024-03-06 10:00:00'),
(4, 2, 2, 1200.00, '2024-06-01', '书本费', '购买教辅资料', 'DISBURSED', 'Voucher-20240601-0001', NULL, 1, '2024-06-01 09:00:00', '情况属实', 3, '2024-06-02 10:00:00'),
(5, 3, 2, 3000.00, '2024-03-10', '学费', '高考冲刺学期学费', 'DISBURSED', 'Voucher-20240310-0001', NULL, 1, '2024-03-10 09:00:00', '情况属实', 3, '2024-03-11 10:00:00'),
(6, 3, 2, 6000.00, '2026-10-15', '学费', '大学预科全年学费及住宿费', 'AUDITED', 'Voucher-20261015-0001', NULL, 1, '2026-10-15 14:30:00', '大额资助，资料核验通过，待复核', NULL, NULL),
(7, 1, 2, 3000.00, '2026-10-16', '生活费', '冬季生活费及取暖补助', 'AUDITED', 'Voucher-20261016-0001', NULL, 1, '2026-10-16 10:00:00', '情况属实，同意资助', NULL, NULL);

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
