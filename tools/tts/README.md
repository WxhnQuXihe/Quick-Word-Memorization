# TTS 朗读方案（暂存，未接入产品）

## 已确认

- 模型：Kokoro-82M ONNX
- 音色：`af_heart`（美音女声）
- 采样率：24kHz
- 输出：MP3 64kbps 单声道
- 生成方式：本地离线批量生成，不做请求时实时推理
- 当前 30 条样音：`_artifacts/tts/samples-kokoro/`
- 样音压缩包：`_artifacts/tts/kokoro-samples-30.zip`

## 正式接入时的数据设计

新增 `word_audio` 表：

```sql
CREATE TABLE word_audio (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  word_id BIGINT UNSIGNED NOT NULL,
  accent VARCHAR(8) NOT NULL DEFAULT 'us',
  voice VARCHAR(64) NOT NULL,
  object_key VARCHAR(255) NOT NULL,
  checksum CHAR(64) NOT NULL,
  duration_ms INT NULL,
  size_bytes INT NULL,
  model VARCHAR(64) NOT NULL,
  model_version VARCHAR(32) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_word_accent_voice (word_id, accent, voice)
);
```

- 匹配键：`word_id + accent + voice`
- 文件路径：`/opt/shorthand/audio/{sha256前2位}/{sha256}.mp3`
- 文件哈希输入：`lower(trim(word))|accent|voice|model_version`
- 音频文件放服务器磁盘，数据库只存索引，不存 BLOB
- 接口返回 `audioUrl`，App 用 `new Audio(url).play()` 播放并预加载下一组

## 批量生成计划

1. 从 `word` 表读取 9,717 个唯一单词
2. `tts_text` 默认等于单词本身；词组按整条读
3. 生成 WAV → ffmpeg 转 MP3
4. 已存在且 checksum 相同的跳过，支持断点续跑
5. 失败写 `failed.csv`
6. 人工试听后把多音词加入 `tts_overrides.tsv`

## 已知问题

- 词组 `a bit (of)` 的括号内容 `of` 会被读出来；正式生成时需要清洗括号或使用 overrides
- `read / lead / present / record / object` 等多音词需要人工确认
- 不要用音标代替 `tts_text`；Kokoro/Piper 常规输入是英文文本，不是 IPA

## 生成命令（样音）

```bash
python tools/tts/generate_kokoro_samples.py
```

模型文件不提交仓库，放在：

```text
_artifacts/tts/kokoro/model.onnx
_artifacts/tts/kokoro/af_heart.bin
```
