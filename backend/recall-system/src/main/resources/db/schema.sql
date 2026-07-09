-- =====================================================================
-- 待办系统建表脚本 (PRD 第 9 章 数据模型)
-- 数据库: MySQL 8.x   字符集: utf8mb4   引擎: InnoDB
-- 时间戳统一 UTC 存储(见 PRD 4.4)；日期字段精确到天。
-- 所有业务表均带 user_id 实现数据隔离(PRD 3.2)，并建相应索引(PRD 9.4 关键索引建议)。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `recall` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `recall`;

-- ---------------------------------------------------------------------
-- 9.1 用户表 users
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`          VARCHAR(20)  NOT NULL COMMENT '用户名，全局唯一',
    `email`             VARCHAR(128) NOT NULL COMMENT '邮箱，全局唯一',
    `password_hash`     VARCHAR(100) NOT NULL COMMENT '密码哈希(BCrypt)',
    `nickname`          VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `timezone`          VARCHAR(40)  DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `reminder_enabled`  TINYINT(1)   DEFAULT 1 COMMENT '提醒总开关',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(UTC)',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ---------------------------------------------------------------------
-- 9.2 分类表 categories（两层合并单表，用 parent_id 表达层级）
-- parent_id IS NULL = 大分类（第一层）；非空 = 子分类（第二层）。
-- 应用层控制最多 2 层；物理删除，无软删除字段。
-- color 仅大分类可用，子分类强制 NULL。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT      NOT NULL COMMENT '所属用户(数据隔离)',
    `parent_id`  BIGINT      DEFAULT NULL COMMENT '父分类ID;NULL=大分类,非空=子分类',
    `name`       VARCHAR(20) NOT NULL COMMENT '名称(同父下唯一)',
    `color`      VARCHAR(20) DEFAULT NULL COMMENT '颜色;仅大分类可用',
    `sort_order` INT         DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    -- 同父下同名唯一；用 generated column 把 parent_id 的 NULL 归一为 0，
    -- 否则 MySQL 多个 NULL 视为不冲突，无法保证"同用户下各大分类名唯一"
    `parent_key` BIGINT      AS (COALESCE(`parent_id`, 0)) STORED,
    UNIQUE KEY `uk_user_parent_name` (`user_id`, `parent_key`, `name`),
    KEY `idx_user_parent` (`user_id`, `parent_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='分类表(两层,合并子分类)';

-- ---------------------------------------------------------------------
-- 9.4 待办表 todos
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `todos`;
CREATE TABLE `todos` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户(数据隔离)',
    `title`         VARCHAR(100) NOT NULL COMMENT '标题',
    `note`          VARCHAR(2000) DEFAULT NULL COMMENT '备注',
    `category_id`   BIGINT       DEFAULT NULL COMMENT '分类ID(指向categories表任意层级节点)',
    `priority`      VARCHAR(10)  NOT NULL DEFAULT '1' COMMENT '优先级 0低/1中/2高',
    `status`        VARCHAR(10)  NOT NULL DEFAULT '0' COMMENT '状态 0待处理/1已完成',
    `done_at`       DATETIME     DEFAULT NULL COMMENT '完成时间(UTC),可空',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    -- PRD 9.4 关键索引
    KEY `idx_user_status_created` (`user_id`, `status`, `created_at`),
    KEY `idx_user_category_status` (`user_id`, `category_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='待办表';

-- ---------------------------------------------------------------------
-- 9.7 月团队冲刺表 sprint_items
-- 冲刺任务可关联多个关键成果 K（通过 sprint_key_results 关联表）；
-- 关联 K 后，K 状态变更会联动同步冲刺状态。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sprint_items`;
CREATE TABLE `sprint_items` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL COMMENT '所属用户(数据隔离)',
    `month`           CHAR(7)      NOT NULL COMMENT '月份 YYYY-MM',
    `title`           VARCHAR(100) NOT NULL COMMENT '冲刺任务标题',
    `status`          VARCHAR(15)  NOT NULL DEFAULT '0' COMMENT '0未开始/1进行中/2已完成',
    `need_involved`   TINYINT(1)   DEFAULT 0 COMMENT '是否需我介入(需介入才可关联K)',
    `note`            VARCHAR(2000) DEFAULT NULL COMMENT '备注/说明',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_month` (`user_id`, `month`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='月团队冲刺表';

-- ---------------------------------------------------------------------
-- 冲刺-关键成果关联表 sprint_key_results（多对多）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sprint_key_results`;
CREATE TABLE `sprint_key_results` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sprint_id`      BIGINT   NOT NULL COMMENT '冲刺任务ID',
    `key_result_id`  BIGINT   NOT NULL COMMENT '关键成果ID',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sprint_kr` (`sprint_id`, `key_result_id`),
    KEY `idx_key_result` (`key_result_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='冲刺-关键成果关联表';

-- ---------------------------------------------------------------------
-- 月度绩效目标表 objectives（v2.0：替代旧 perf_categories/perf_items）
-- 目标 O：进度/状态/完成时间均由其下 key_results 派生计算，本表不存这些字段。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `objectives`;
CREATE TABLE `objectives` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户(数据隔离)',
    `month`       CHAR(7)      NOT NULL COMMENT '月份 YYYY-MM',
    `name`        VARCHAR(100) NOT NULL COMMENT '目标名称',
    `description` VARCHAR(2000) DEFAULT NULL COMMENT '目标描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_month` (`user_id`, `month`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='月度绩效目标表';

-- ---------------------------------------------------------------------
-- 关键成果表 key_results（归属于目标 O）
-- complete_date 由后端管理：status→done 填当天，切回非done 清空。
-- cancel_reason 由后端管理：status→cancelled 填入，切回非cancelled 清空。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `key_results`;
CREATE TABLE `key_results` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`            BIGINT       NOT NULL COMMENT '所属用户(数据隔离)',
    `objective_id`       BIGINT       NOT NULL COMMENT '归属目标O',
    `name`               VARCHAR(100) NOT NULL COMMENT '关键成果名称',
    `description`        VARCHAR(2000) DEFAULT NULL COMMENT '描述',
    `status`             VARCHAR(15)  NOT NULL DEFAULT '0' COMMENT '状态 0未开始/1进行中/2已完成/3已取消',
    `plan_complete_date` DATE         DEFAULT NULL COMMENT '计划完成时间',
    `complete_date`      DATE         DEFAULT NULL COMMENT '实际完成时间(后端管理)',
    `cancel_reason`      VARCHAR(500) DEFAULT NULL COMMENT '取消原因(状态为已取消时填,后端管理)',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_objective_status` (`user_id`, `objective_id`, `status`),
    KEY `idx_objective` (`objective_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='关键成果表';

-- ---------------------------------------------------------------------
-- 关键成果成果记录 R 表（月度绩效 v2.1）
-- K 切换到「已完成」时由用户提交的成果记录，1:N，仅 content 文本。
-- 切回进行中/取消时保留 R；删 K 时级联物理删除 R。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `key_result_records`;
CREATE TABLE `key_result_records` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户(数据隔离)',
    `key_result_id` BIGINT       NOT NULL COMMENT '归属关键成果K',
    `content`       VARCHAR(2000) NOT NULL COMMENT '成果记录内容',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_key_result` (`key_result_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='关键成果成果记录R表';
