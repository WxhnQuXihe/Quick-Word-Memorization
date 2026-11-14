# Shorthand

神也背单词 · 背单词 App（个人/小范围使用，不商业化）。

技术栈：Vue 3 + Vite + Capacitor（Android） / Spring Boot 3 + MyBatis-Plus + MySQL 8。

## 当前状态

用户端、管理后台、Android App、词书导入与生产部署已完成。

## 架构与模型方案（前 8 条）

1. **定位**：个人/小范围使用的背单词工具，不开放注册，用户由管理后台创建。
2. **学习模型**：词书按轮次刷；每轮把未标熟的词随机打乱，每组随机取 10 个；最后一组不足 10 个就展示实际数量。
3. **熟词替换**：点击「标熟」后立即从本书未标熟、且不在当前组的词里随机替换 1 个；无词可替换时该位置置空。
4. **下一组**：两段式防误触；第一次点击只进入 3 秒待确认状态，第二次点击才切组。
5. **隐藏交互**：单词区和释义区可分别点击隐藏；底部保留「隐藏单词 / 隐藏释义」全局按钮，单卡与全局状态互不覆盖。
6. **技术栈**：用户端 Vue 3 + Vite + TypeScript + Vant 4 + Capacitor；管理后台 Vue 3 + Element Plus；后端 Spring Boot 3 + MyBatis-Plus + MySQL 8。
7. **数据模型**：用户、词书、词条、词书-词关联、学习组、收藏/熟词、反馈、导入记录、公告等 12 张表；多词性释义存在 `word.meaning`，一行一个词性，不额外拆表。
8. **明确不做**：遗忘曲线、每日目标、复习队列、TTS 读音、注册、支付、社区。

## 目录

```
server/     Spring Boot 后端
app/        Vue 3 用户端（Capacitor Android）
admin/      Vue 3 管理后台
db/         建表脚本
assets/     图标与启动页
```

## 本地启动

```bash
# 后端（8081）
cd server
MYSQL_USERNAME=root MYSQL_PASSWORD=root SERVER_PORT=8081 java -jar target/shorthand-server-0.1.0.jar

# 管理后台（5174）
cd admin && npm install && npm run dev

# 用户端（5173）
cd app && npm install && npm run dev
```

首次建库：`mysql -uroot -p < db/schema.sql`（库名 `shorthand`）。

## 发布

每次用户端有可见更新，必须打版本 tag，自动构建 Android Release APK 并发布到 GitHub Release：

```bash
tools/release.sh v0.4.0
```
