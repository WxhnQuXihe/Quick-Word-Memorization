-- 2026-11-27: word 作为 lexeme 兼容表 + word_entry/sense 词书义项模型
-- 仅新增/放宽结构，不删除用户账号，不修改既有学习数据。
ALTER TABLE word
    ADD COLUMN headword_key VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '大小写敏感的原始词头键' AFTER normalized,
    ADD COLUMN pronunciation_key VARCHAR(64) NOT NULL DEFAULT '' COMMENT '标准化读音键，优先美音' AFTER headword_key;

ALTER TABLE word DROP INDEX uk_normalized;
ALTER TABLE word ADD UNIQUE KEY uk_word_identity (normalized, pronunciation_key, headword_key);

CREATE TABLE IF NOT EXISTS word_entry (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    word_id     BIGINT UNSIGNED NOT NULL COMMENT 'lexeme / word.id',
    entry_hash  CHAR(64)        NOT NULL COMMENT 'sha256(排序后的 sense_id 列表)',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_word_entry_hash (word_id, entry_hash),
    KEY idx_word (word_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词条（义项集合）';

CREATE TABLE IF NOT EXISTS sense (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    word_id     BIGINT UNSIGNED NOT NULL COMMENT 'lexeme / word.id',
    pos         VARCHAR(32)     NOT NULL COMMENT '展示词性',
    pos_key     VARCHAR(32)     NOT NULL COMMENT '归一化词性键',
    meaning     TEXT            NOT NULL COMMENT '展示释义',
    meaning_key CHAR(64)        NOT NULL COMMENT '词性 + 排序同义词集合的 sha256',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sense_key (word_id, pos_key, meaning_key),
    KEY idx_word (word_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '义项';

CREATE TABLE IF NOT EXISTS word_entry_sense (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entry_id        BIGINT UNSIGNED NOT NULL,
    sense_id        BIGINT UNSIGNED NOT NULL,
    sort            INT             NOT NULL DEFAULT 0,
    display_meaning TEXT            DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entry_sense (entry_id, sense_id),
    KEY idx_sense (sense_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '词条义项';

ALTER TABLE wordbook_word
    ADD COLUMN entry_id BIGINT UNSIGNED DEFAULT NULL COMMENT '本书使用的 word_entry.id' AFTER word_id,
    ADD UNIQUE KEY uk_book_entry (wordbook_id, entry_id),
    ADD KEY idx_entry (entry_id);
