# Shortand

神也背单词 · 背单词 App（个人/小范围使用，不商业化）。

技术栈：Vue 3 + Vite + Capacitor（iOS/Android） / Spring Boot 3 + MyBatis-Plus + MySQL 8。

## 当前阶段

后端核心接口、用户端核心页、管理后台壳已完成；下一步接词书/词条 CRUD、Excel 导入和 Capacitor 原生工程。

## 文档

- [架构与数据模型方案](docs/01-架构与数据模型方案.md)
- [部署方案（47.95.66.191，8082）](docs/02-部署方案.md)
- [UI 方案（含三套原型）](docs/03-UI方案.md)
- [管理后台 UI 方案](docs/04-管理后台UI方案.md)
- [数据库建表脚本](db/schema.sql)
- [开发大纲与进度](docs/05-开发大纲与进度.md)

## 目录

```
server/     Spring Boot 后端
app/        Vue 3 用户端（Capacitor）
admin/      Vue 3 管理后台
db/         建表脚本
docs/       设计与部署文档
prototype/  交互原型
assets/     图标与启动页
```

## 本地启动

```bash
# 后端（8081）
cd server
MYSQL_USERNAME=root MYSQL_PASSWORD=root SERVER_PORT=8081 java -jar target/shortand-server-0.1.0.jar

# 管理后台（5174）
cd admin && npm install && npm run dev

# 用户端（5173）
cd app && npm install && npm run dev
```

首次建库：`mysql -uroot -p < db/schema.sql`（库名 `shortand`）。
