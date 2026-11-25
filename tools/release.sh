#!/usr/bin/env bash
# 用户端可见更新后发布 Android APK。
# 用法：tools/release.sh v0.3.1
set -euo pipefail
TAG="${1:?用法: tools/release.sh vX.Y.Z}"
NOTES=".github/release-notes/${TAG}.md"

if [[ ! -f "$NOTES" ]]; then
  echo "缺少发布说明：$NOTES" >&2
  echo "先写清楚这个版本修复/新增了什么，再打 tag。" >&2
  exit 1
fi

git tag -a "$TAG" -F "$NOTES"
git push origin "$TAG"
echo "已推送 $TAG，GitHub Actions 将使用 $NOTES 作为 Release 正文并构建 Android APK。"
