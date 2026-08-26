-- 意大利语A2词汇学习系统建表脚本（幂等，可重复执行）

CREATE TABLE IF NOT EXISTS `word` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `word`         VARCHAR(64)  NOT NULL COMMENT '单词',
    `pos`          VARCHAR(32)  NULL COMMENT '词性',
    `meaning`      VARCHAR(255) NULL COMMENT '中文释义',
    `category`     VARCHAR(64)  NULL COMMENT '主题分类',
    `gender`       VARCHAR(8)   NULL COMMENT '名词性别 m/f',
    `article`      VARCHAR(32)  NULL COMMENT '定冠词 il/lo/la/l''/i/gli/le',
    `conjugation`  TEXT         NULL COMMENT '动词变位 JSON',
    `adj_forms`    TEXT         NULL COMMENT '形容词性数变化 JSON {ms,fs,mp,fp}',
    `plural`       VARCHAR(64)  NULL COMMENT '名词复数形式',
    `example`      TEXT         NULL COMMENT '例句 JSON {it,zh}',
    `created_at`   DATETIME     NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '单词表';

CREATE TABLE IF NOT EXISTS `word_progress` (
    `id`                 BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `word_id`            BIGINT  NOT NULL COMMENT '单词ID',
    `extract_count`      INT     NOT NULL DEFAULT 0 COMMENT '累计抽取次数（标记完成后+1）',
    `last_extracted_at`  DATE    NULL COMMENT '最近一次被抽取的日期',
    `status`             TINYINT NOT NULL DEFAULT 0 COMMENT '0=从未抽取 1=已抽取未完成 2=已完成',
    `completed_at`       DATETIME NULL COMMENT '最近完成时间',
    `box`                INT     NOT NULL DEFAULT 0 COMMENT 'SRS盒子级别0-5',
    `next_review_at`     DATE    NULL COMMENT '下次复习日期',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word_id` (`word_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学习进度表';

CREATE TABLE IF NOT EXISTS `daily_extract` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `extract_date` DATE     NOT NULL COMMENT '抽取日期',
    `word_id`      BIGINT   NOT NULL COMMENT '单词ID',
    `status`       TINYINT  NOT NULL DEFAULT 0 COMMENT '0=未完成 1=已完成',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_word` (`extract_date`, `word_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每日抽取记录表';

CREATE TABLE IF NOT EXISTS `setting` (
    `id`            BIGINT NOT NULL COMMENT '主键（固定为1，单行）',
    `daily_count`   INT    NOT NULL DEFAULT 35 COMMENT '每日抽取数量',
    `cooldown_days` INT    NOT NULL DEFAULT 7 COMMENT '冷却天数',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设置表';

-- 默认设置行（幂等写入）
INSERT INTO `setting` (`id`, `daily_count`, `cooldown_days`)
VALUES (1, 35, 7)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ===== SRS 间隔复习 + 例句字段迁移（幂等：列已存在时跳过）=====
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema + 动态 SQL 实现
-- 已有库的手动等价语句：
--   ALTER TABLE word_progress
--       ADD COLUMN box INT NOT NULL DEFAULT 0 COMMENT 'SRS盒子级别0-5',
--       ADD COLUMN next_review_at DATE NULL COMMENT '下次复习日期';
--   ALTER TABLE word
--       ADD COLUMN example TEXT NULL COMMENT '例句 JSON {it,zh}';

-- word_progress.box
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `word_progress` ADD COLUMN `box` INT NOT NULL DEFAULT 0 COMMENT ''SRS盒子级别0-5''',
    'SELECT 1')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'word_progress' AND COLUMN_NAME = 'box');
PREPARE migrate_stmt FROM @ddl;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;

-- word_progress.next_review_at
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `word_progress` ADD COLUMN `next_review_at` DATE NULL COMMENT ''下次复习日期''',
    'SELECT 1')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'word_progress' AND COLUMN_NAME = 'next_review_at');
PREPARE migrate_stmt FROM @ddl;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;

-- word.example
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `word` ADD COLUMN `example` TEXT NULL COMMENT ''例句 JSON {it,zh}''',
    'SELECT 1')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'word' AND COLUMN_NAME = 'example');
PREPARE migrate_stmt FROM @ddl;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;
