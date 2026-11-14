#!/usr/bin/env bash
# 用户端可见更新后发布 Android APK。
# 用法：tools/release.sh v0.3.1
set -euo pipefail
TAG="${1:?用法: tools/release.sh vX.Y.Z}"
git tag -a "$TAG" -m "Shorthand $TAG"
git push origin "$TAG"
echo "已推送 $TAG，GitHub Actions 将自动构建 Android APK 并发布 Release。"
