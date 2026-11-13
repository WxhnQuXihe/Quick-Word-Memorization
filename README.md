# shorthand

神也背单词 · 背单词 App（个人/小范围使用，不商业化）。

技术栈：Vue 3 + Vite + Capacitor（iOS/Android） / Spring Boot 3 + MyBatis-Plus + MySQL 8。

## 当前阶段

方案设计完成，准备搭工程骨架。

## 文档

- [架构与数据模型方案](docs/01-架构与数据模型方案.md)
- [部署方案（47.95.66.191，8082）](docs/02-部署方案.md)
- [UI 方案（含三套原型）](docs/03-UI方案.md)
- [数据库建表脚本](db/schema.sql)

## 规划目录

```
server/     Spring Boot 后端
app/        Vue 3 用户端（Capacitor）
admin/      Vue 3 管理后台
db/         建表脚本
docs/       设计与部署文档
wordbooks/  词书源文件（后续整理）
```
