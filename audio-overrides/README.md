# 人工音频覆盖

只放人工修正过的 MP3，不放自动生成的 6,073 条全量音频。

添加一条：

```bash
python tools/audio/make_override.py --word abandon --file ~/Downloads/abandon.mp3
git add audio-overrides
git commit -m "audio: 修正 abandon"
git push
```

推送后会触发 `.github/workflows/audio-overrides.yml`，只同步本次变更的文件到服务器，并通过后台接口更新 `word_audio`。

需要仓库 Secrets：

- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`

规则：

- 只支持 MP3，最大 5MB
- 一个 word_id 只保留一个修正文件
- 文件名使用 `word_id-slug.mp3`
