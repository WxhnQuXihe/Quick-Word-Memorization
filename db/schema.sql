-- ============================================================================
-- Shorthand · 背单词 App —— 数据库结构 v0.4（MySQL 8.0 / utf8mb4_unicode_ci）
-- 服务器：47.95.66.191，与 setbook 共用 MySQL，独立库 shorthand
-- 设计：不做遗忘曲线 / 不做学习规划；每组 10 词，熟词即时随机替换
-- 说明：业务代码不写 SQL，全部用 MyBatis-Plus Wrapper；本文件只用于首次建库
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `shorthand`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `shorthand`;

-- ---------------------------------------------------------------------------
-- 1. C 端用户（不开放注册，由后台创建）
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  username         VARCHAR(32)     NOT NULL                COMMENT '登录账号',
  password         VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密码',
  nickname         VARCHAR(32)     DEFAULT NULL            COMMENT '昵称',
  avatar           VARCHAR(255)    DEFAULT NULL            COMMENT '头像 URL',
  phone            VARCHAR(20)     DEFAULT NULL            COMMENT '手机号，可空',
  status           TINYINT         NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
  reminder_enabled TINYINT         NOT NULL DEFAULT 0      COMMENT '学习提醒开关',
  reminder_time    TIME            NOT NULL DEFAULT '20:30:00',
  theme            VARCHAR(8)      NOT NULL DEFAULT 'system',
  last_login_time  DATETIME        DEFAULT NULL,
  create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'C 端用户';

-- ---------------------------------------------------------------------------
-- 2. 词书（当前五本：高考 / 专升本 / 四级 / 六级 / 考研）
-- ---------------------------------------------------------------------------
CREATE TABLE wordbook (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(32)     NOT NULL                COMMENT 'cet4/cet6/phrase',
  name        VARCHAR(64)     NOT NULL,
  description VARCHAR(512)    DEFAULT NULL,
  cover       VARCHAR(255)    DEFAULT NULL,
  difficulty  TINYINT         NOT NULL DEFAULT 3      COMMENT '1~5',
  word_count  INT             NOT NULL DEFAULT 0      COMMENT '发布时刷新',
  sort        INT             NOT NULL DEFAULT 100,
  status      TINYINT         NOT NULL DEFAULT 0      COMMENT '0 草稿 1 上架',
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词书';

-- ---------------------------------------------------------------------------
-- 3. 单词（读音身份 lexeme，五本词书共享）
-- ---------------------------------------------------------------------------
CREATE TABLE word (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  word        VARCHAR(64)     NOT NULL                COMMENT '单词/词组；作为 lexeme 的显示词头',
  normalized  VARCHAR(64)     NOT NULL                COMMENT '小写、NFKC、空格合并后的查重键',
  headword_key VARCHAR(64)    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '大小写敏感的原始词头键；march/March 分开',
  pronunciation_key VARCHAR(64) NOT NULL DEFAULT ''    COMMENT '标准化读音键；优先美音',
  phonetic_uk VARCHAR(64)     DEFAULT NULL,
  phonetic_us VARCHAR(64)     DEFAULT NULL,
  pos         VARCHAR(32)     DEFAULT NULL            COMMENT '首个词性，如 v./n.；完整词性见 meaning 多行',
  meaning     TEXT            NOT NULL                COMMENT '释义，一行一个词性：v. 放弃...\nn. 放任...',
  example_en  TEXT            DEFAULT NULL,
  example_cn  TEXT            DEFAULT NULL,
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_word_identity (normalized, pronunciation_key, headword_key),
  KEY idx_word (word)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '单词词条（兼容 lexeme）';

-- ---------------------------------------------------------------------------
-- 3.1 词条（word_entry）：一条记录代表一个词书里的完整义项集合
-- ---------------------------------------------------------------------------
CREATE TABLE word_entry (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  word_id     BIGINT UNSIGNED NOT NULL                COMMENT 'lexeme / word.id',
  entry_hash  CHAR(64)        NOT NULL                COMMENT 'sha256(排序后的 sense_id 列表)',
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_word_entry_hash (word_id, entry_hash),
  KEY idx_word (word_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词条（义项集合）';

-- ---------------------------------------------------------------------------
-- 3.2 义项（sense）：全局复用，按 word + 词性 + 释义键去重
-- ---------------------------------------------------------------------------
CREATE TABLE sense (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  word_id     BIGINT UNSIGNED NOT NULL                COMMENT 'lexeme / word.id',
  pos         VARCHAR(32)     NOT NULL                COMMENT '展示词性，如 n. / v. / det.',
  pos_key     VARCHAR(32)     NOT NULL                COMMENT '归一化词性键',
  meaning     TEXT            NOT NULL                COMMENT '展示释义（保留同义组顺序）',
  meaning_key CHAR(64)        NOT NULL                COMMENT '词性 + 排序同义词集合的 sha256',
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sense_key (word_id, pos_key, meaning_key),
  KEY idx_word (word_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '义项';

-- ---------------------------------------------------------------------------
-- 3.3 词条-义项（有序展示）
-- ---------------------------------------------------------------------------
CREATE TABLE word_entry_sense (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entry_id        BIGINT UNSIGNED NOT NULL,
  sense_id        BIGINT UNSIGNED NOT NULL,
  sort            INT             NOT NULL DEFAULT 0,
  display_meaning TEXT            DEFAULT NULL        COMMENT '本书展示用释义，保留同义组顺序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_entry_sense (entry_id, sense_id),
  KEY idx_sense (sense_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词条义项';

-- ---------------------------------------------------------------------------
-- 4. 词书-单词 关联（书内顺序）
-- ---------------------------------------------------------------------------
CREATE TABLE wordbook_word (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  wordbook_id BIGINT UNSIGNED NOT NULL,
  word_id     BIGINT UNSIGNED NOT NULL                COMMENT 'lexeme / word.id',
  entry_id    BIGINT UNSIGNED DEFAULT NULL            COMMENT '本书使用的 word_entry.id',
  seq         INT             NOT NULL                COMMENT '书内顺序，从 1 开始',
  unit        VARCHAR(32)     DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_book_seq (wordbook_id, seq),
  UNIQUE KEY uk_book_word (wordbook_id, word_id),
  UNIQUE KEY uk_book_entry (wordbook_id, entry_id),
  KEY idx_word (word_id),
  KEY idx_entry (entry_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词书单词关联';

-- ---------------------------------------------------------------------------
-- 5. 用户书架（当前刷到第几轮）
-- ---------------------------------------------------------------------------
CREATE TABLE user_wordbook (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id          BIGINT UNSIGNED NOT NULL,
  wordbook_id      BIGINT UNSIGNED NOT NULL,
  current_round_no INT             NOT NULL DEFAULT 1  COMMENT '当前轮次，从 1 开始',
  last_study_time  DATETIME        DEFAULT NULL,
  create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_book (user_id, wordbook_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户书架';

-- ---------------------------------------------------------------------------
-- 6. 学习组（每组固定 10 词；点"下一组"才创建下一组）
-- ---------------------------------------------------------------------------
CREATE TABLE study_group (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  wordbook_id BIGINT UNSIGNED NOT NULL,
  round_no    INT             NOT NULL DEFAULT 1      COMMENT '第几轮',
  group_no    INT             NOT NULL DEFAULT 1      COMMENT '本轮第几组',
  word_count  TINYINT         NOT NULL DEFAULT 10     COMMENT '本组词数（最后一组可能不足 10）',
  status      TINYINT         NOT NULL DEFAULT 0      COMMENT '0 进行中 1 已完成',
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time DATETIME        DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_book_round_group (user_id, wordbook_id, round_no, group_no),
  KEY idx_user_status (user_id, status),
  KEY idx_book_round (wordbook_id, round_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习组';

-- ---------------------------------------------------------------------------
-- 7. 组内单词（position 固定 1~10；熟词被替换时直接换掉该行的 word_id）
-- ---------------------------------------------------------------------------
CREATE TABLE study_group_word (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  group_id    BIGINT UNSIGNED NOT NULL,
  word_id     BIGINT UNSIGNED DEFAULT NULL            COMMENT '为空表示无可替换词',
  position    TINYINT         NOT NULL                COMMENT '1~10',
  status      TINYINT         NOT NULL DEFAULT 1      COMMENT '1 正常 2 无词可补',
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_position (group_id, position),
  UNIQUE KEY uk_group_word (group_id, word_id),
  KEY idx_word (word_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '组内单词';

-- ---------------------------------------------------------------------------
-- 8. 用户-单词状态（收藏 / 熟词 / 见过次数，全局唯一）
-- ---------------------------------------------------------------------------
CREATE TABLE user_word (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id           BIGINT UNSIGNED NOT NULL,
  word_id           BIGINT UNSIGNED NOT NULL,
  is_favorite       TINYINT         NOT NULL DEFAULT 0,
  favorite_time     DATETIME        DEFAULT NULL,
  is_known          TINYINT         NOT NULL DEFAULT 0   COMMENT '标熟',
  known_time        DATETIME        DEFAULT NULL,
  seen_count        INT             NOT NULL DEFAULT 0,
  last_seen_time    DATETIME        DEFAULT NULL,
  first_wordbook_id BIGINT UNSIGNED DEFAULT NULL,
  create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_word (user_id, word_id),
  KEY idx_favorite (user_id, is_favorite, favorite_time),
  KEY idx_known (user_id, is_known, known_time),
  KEY idx_word (word_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户单词状态';

-- ---------------------------------------------------------------------------
-- 9. 意见反馈
-- ---------------------------------------------------------------------------
CREATE TABLE feedback (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED DEFAULT NULL,
  content     TEXT            NOT NULL,
  images      JSON            DEFAULT NULL,
  contact     VARCHAR(64)     DEFAULT NULL,
  status      TINYINT         NOT NULL DEFAULT 0      COMMENT '0 待处理 1 已回复 2 关闭',
  reply       TEXT            DEFAULT NULL,
  reply_time  DATETIME        DEFAULT NULL,
  create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '意见反馈';

-- ---------------------------------------------------------------------------
-- 10. 后台管理员（只有一个账号）
-- ---------------------------------------------------------------------------
CREATE TABLE admin_user (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username        VARCHAR(32)     NOT NULL,
  password        VARCHAR(100)    NOT NULL,
  name            VARCHAR(32)     DEFAULT NULL,
  status          TINYINT         NOT NULL DEFAULT 1,
  last_login_time DATETIME        DEFAULT NULL,
  create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '后台管理员';

-- ---------------------------------------------------------------------------
-- 11. 导入记录
-- ---------------------------------------------------------------------------
CREATE TABLE import_log (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  wordbook_id  BIGINT UNSIGNED NOT NULL,
  file_name    VARCHAR(255)    NOT NULL,
  total_rows   INT             NOT NULL DEFAULT 0,
  success_rows INT             NOT NULL DEFAULT 0,
  fail_rows    INT             NOT NULL DEFAULT 0,
  status       TINYINT         NOT NULL DEFAULT 0    COMMENT '0 处理中 1 成功 2 部分失败 3 失败',
  error_file   VARCHAR(255)    DEFAULT NULL,
  admin_id     BIGINT UNSIGNED DEFAULT NULL,
  create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time  DATETIME        DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wordbook (wordbook_id),
  KEY idx_create (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词书导入记录';

-- ---------------------------------------------------------------------------
-- 初始化管理员：admin / 123456（BCrypt），上线后立即改密码
-- ---------------------------------------------------------------------------
INSERT INTO admin_user (username, password, name)
VALUES ('admin', '$2a$10$LDI3JOpiWOwtEn2qiP2kGeXZv3IMn4kBrAuYW059.0dG5DSfhXONW', '管理员')
ON DUPLICATE KEY UPDATE username = username;

-- 12. 公告
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1发布',
  `publish_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告';

-- 13. 单词音频
CREATE TABLE IF NOT EXISTS `word_audio` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `word_id` BIGINT UNSIGNED NOT NULL,
  `accent` VARCHAR(8) NOT NULL DEFAULT 'us',
  `voice` VARCHAR(64) NOT NULL,
  `object_key` VARCHAR(255) NOT NULL,
  `checksum` CHAR(64) NOT NULL,
  `duration_ms` INT DEFAULT NULL,
  `size_bytes` INT DEFAULT NULL,
  `model` VARCHAR(64) NOT NULL,
  `model_version` VARCHAR(32) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0缺失 1生成中 2完成 3失败',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word_accent_voice` (`word_id`, `accent`, `voice`),
  KEY `idx_checksum` (`checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词音频';
