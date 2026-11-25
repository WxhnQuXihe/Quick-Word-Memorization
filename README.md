# Shorthand · 神也背单词

面向个人与小范围使用的背单词应用：一个 **Android 客户端**、一个 **Web 管理后台**、一个 **Spring Boot 单体后端**，数据落 **MySQL 8**。

不开放注册，不设每日目标，不排复习队列 —— 只做一件事：把一本词书按轮次刷完。

> 项目名 `Shorthand`，App 显示名「神也背单词」。仓库为 monorepo，用户端、管理后台、服务端共用一套 REST API。
> 当前版本：**v0.5.2**（Android 签名包；修复熟词本/收藏夹只显示前 20 条，补充分页浏览与学习取词加固）。
> 本文按当前代码写成，不是方案文档的转述；两者冲突时以代码为准。

## 目录

1. [项目定位](#1-项目定位)
2. [功能范围](#2-功能范围)
3. [技术栈](#3-技术栈)
4. [仓库结构](#4-仓库结构)
5. [运行时架构](#5-运行时架构)
6. [学习模型](#6-学习模型)
7. [数据模型](#7-数据模型)
8. [后端设计](#8-后端设计)
9. [接口一览](#9-接口一览)
10. [前端结构](#10-前端结构)
11. [本地运行](#11-本地运行)
12. [部署](#12-部署)
13. [安卓发布](#13-安卓发布)
14. [词书导入](#14-词书导入)
15. [开发约定](#15-开发约定)
16. [已知限制](#16-已知限制)
17. [许可](#17-许可)

---

## 1. 项目定位

- **使用者**：作者本人与少数朋友。账号由管理后台创建，没有注册入口。
- **形态**：主力是安卓 App（Capacitor 打包的 Vue 3 页面），另有一套只给管理员用的 Web 后台。
- **取舍**：刻意不做「科学背单词」那套机制 —— 没有遗忘曲线、没有每日目标、没有复习队列。已接入**预生成单词朗读**（Kokoro），不做实时 TTS、跟读评分和例句朗读。产品契约只有一条：**一轮过完，就是过完了**。熟词不再出现，剩下的词随机分组反复刷。
- **规模假设**：单实例、单管理员、单本词书最大约 5500 词。所有复杂选词都在 JVM 内存里算，不依赖缓存与队列。

## 2. 功能范围

| 端 | 已实现 |
|---|---|
| 用户端（App / 移动端 Web） | 登录；词书选择与进度；10 词一组背单词；标熟即时换词；收藏；收藏夹（可再背、20 条/页）；熟词本（只读、20 条/页、可移出）；主页概览与连续学习天数；公告；意见反馈与回复查看；单词朗读；版本更新提示 |
| 管理后台 | 总览看板（KPI、词书表、近 14 天活跃度、最近用户、最近导入、JVM 与运行时长）；词书增删改与上下架；词条增删改查、试听与替换单词音频；Excel 批量导入与导入记录；用户建号、重置密码、删除（级联清理学习记录）；反馈查看与回复；公告发布 |
| 明确不做 | 注册、支付、社区、实时 TTS / 跟读评分、遗忘曲线、每日目标、复习队列、多管理员、多租户、iOS 维护（工程保留） |

## 3. 技术栈

| 层 | 选型 |
|---|---|
| 用户端 | Vue 3.5 + Vite + TypeScript + Pinia + Vue Router 4 + Vant 4，自研 Design Token |
| App 壳 | Capacitor 6（Android 为主，iOS 工程保留不维护） |
| 管理后台 | Vue 3.5 + Vite + TypeScript + Element Plus 2.9 |
| 服务端 | Spring Boot 3.2.12 / Java 17，MyBatis-Plus 3.5.7，EasyExcel，Knife4j 4.5.0，Lombok |
| 数据库 | MySQL 8（库名 `shorthand`，`utf8mb4_unicode_ci`） |
| 单词音频 | Kokoro-82M ONNX 离线生成 MP3，Nginx 静态分发 |
| 鉴权 | 自研 HMAC 无状态 token（见 §8.3），未引入 Sa-Token / Spring Security |
| 网关 | 反向代理（Nginx）承接静态站点与 `/api` 转发 |
| CI/CD | GitHub Actions（自动部署 + Android 签名包发布） |

> 项目里**没有** Redis、消息队列、Sa-Token、任何缓存层。这些不是遗漏，是刻意保持单进程可跑。

## 4. 仓库结构

```
.
├─ server/                  Spring Boot 单体后端（Maven）
│  └─ src/main/java/com/shorthand/
│     ├─ common/            Result / PageResult / BizException / GlobalExceptionHandler
│     ├─ config/            MybatisPlusConfig / WebConfig / AuthInterceptor / TokenStore
│     ├─ controller/        用户端接口 + admin/ 子包（管理端接口）
│     ├─ domain/            实体 + bo/（入参）+ vo/（出参）
│     ├─ mapper/            13 个 Mapper，全部 extends BaseMapper<T>
│     ├─ service/ impl/     业务实现（StudyServiceImpl 承载核心学习模型）
│     └─ listener/          WordImportListener（EasyExcel 读表）
├─ app/                     Vue 3 用户端（Capacitor 壳 + android/ ios/ 原生工程）
├─ admin/                   Vue 3 管理后台（base 为 /admin/）
├─ db/schema.sql            首次建库脚本（13 张表，执行一次即弃）
├─ tools/                   词书转换、Kokoro 音频生成、音标校对、冒烟测试、版本发布脚本
├─ assets/                  App 图标与启动页
├─ .github/workflows/       deploy.yml / release.yml / android.yml
└─ AGENTS.md                面向编码 Agent 的仓库说明
```

词书原始 PDF / Excel、设计文档、部署物料（systemd 单元、反向代理站点、备份脚本、环境变量样例）属于本地私有素材，不纳入版本库。

## 5. 运行时架构

```
   Android APK                          浏览器
 (Capacitor 壳)                 (用户端 Web / 管理后台)
        │                                 │
        └───────────────┬─────────────────┘
                        ▼
               ┌─────────────────┐
               │  反向代理        │   /        → 用户端构建产物（单页）
               │                 │   /admin/  → 管理后台构建产物（单页）
               │                 │   /uploads → 上传文件目录
               │                 │   /audio   → 单词 MP3 静态目录（Nginx alias）
               └────────┬────────┘
                        │  /api/ 转发
               ┌────────▼────────┐
               │  Spring Boot    │  默认端口 8081（SERVER_PORT）
               │  单体 · 无状态   │  业务全部同步执行
               └────────┬────────┘
                        │  JDBC
               ┌────────▼────────┐
               │   MySQL 8       │  库 shorthand
               └─────────────────┘
```

- 一个后端进程同时服务两端，靠 URI 前缀区分身份：`/api/admin/**` 走管理员 token，其余走用户 token。
- 服务端完全无状态（token 是签名后的自包含串），因此可随时重启、重新部署不影响登录态。
- 前端构建产物是静态文件，由反向代理直接吐；只有 `/api` 会打到 Java 进程。

## 6. 学习模型

这是整个项目唯一「有设计」的地方，其余都是常规 CRUD。

### 6.1 三个概念

| 概念 | 含义 |
|---|---|
| **轮次 round** | 把词书里「未标熟」的词全部走一遍算一轮。轮次只是「从头再刷」的边界，不含任何时间调度 |
| **组 group** | 一轮中的一批词，默认 10 个，最后一组可能不足 10 个 |
| **位置 position** | 一组内 1..10 的槽位。标熟时是**替换槽位上的词**，不是删除槽位 |

每组落两张表：`study_group`（组头：轮次、组号、状态、完成时间）与 `study_group_word`（槽位：position → word_id + status）。

### 6.2 取当前组

`GET /api/study/current?wordbookId=` 最终走 `StudyServiceImpl#ensureGroup`：

1. 查该书当前轮次下是否已有 `status = 0` 的进行中组，有则直接返回（幂等，可随便刷新）。
2. 没有则取候选池：**本书全部 wordId − 已标熟 wordId − 本轮已出现过的 wordId**。
3. 候选为空 → `current_round_no + 1`，用新一轮的条件重新取；仍为空 → 返回 `finished: true`，全书已熟。
4. 候选池 `Collections.shuffle` 打乱后取前 `min(10, size)` 个，写入组与槽位，并给这些词 `seen_count + 1`。

> 「本轮已出现过的词」由该轮所有组的 word_id 聚合而来，因此**一轮之内不会重复出词**；换轮才重新回到池子。

### 6.3 下一组

`POST /api/study/group/{id}/next`：把当前组置为 `status = 1` 并记 `finish_time`，然后复用 §6.2 的流程取下一组（组号 +1；本轮取完则进入下一轮）。

- **幂等**：对已完成的组再次调用，返回的是当前进行中的组，不会丢组、不会重复计进度。
- 前端在此基础上加了两段式防误触：第一次点击只进入 3 秒待确认态（按钮变色、倒计时），第二次点击才真正发请求，超时自动取消。

### 6.4 标熟 → 即时随机换词

`POST /api/study/group/{id}/known/{wordId}`：

1. 校验该词确实在当前组的有效槽位上，否则报错。
2. `user_word.is_known = 1`，记 `known_time`。
3. 候选池 = **本书全部词 − 已标熟 − 当前组**（不排除本轮其他组，所以可能抽到本轮前几组背过的词，等同于复习）。
4. 有候选 → 随机取 1 个，替换该 position 上的 `word_id`，`seen_count + 1`，把新词回给前端；前端只换这一张卡，不刷新整组。
5. 无候选 → 该 position 置 `status = 2`、`word_id = NULL`，接口返回 `null`，前端提示「本书已全部标熟」。

收藏只打标记，**不触发替换**。

### 6.5 进度口径

- 词书进度 = 该书 `seen_count > 0` 的词数 / `wordbook.word_count`。
- 收藏数、熟词数取自 `user_word` 的全局计数，主页、我的、收藏夹、熟词本同源，不建统计表。
- 主页额外给 `streakDays`（连续学习天数）：取已完成学习组的完成日期往前数连续天数，不做「打卡」功能。

## 7. 数据模型

`db/schema.sql` 共 13 张表，全部 `utf8mb4_unicode_ci`。

| 表 | 作用 | 关键字段 |
|---|---|---|
| `app_user` | 用户（后台创建） | username / password / nickname / status / deleted |
| `admin_user` | 管理员（单账号） | username / password / last_login_time |
| `wordbook` | 词书 | code / name / word_count / sort / status / deleted |
| `word` | 全局词条 | word / normalized / phonetic_uk / phonetic_us / pos / meaning / deleted |
| `wordbook_word` | 词书-词关联 | wordbook_id / word_id / seq / unit |
| `user_wordbook` | 用户书架与轮次游标 | user_id / wordbook_id / current_round_no / last_study_time |
| `study_group` | 学习组 | user_id / wordbook_id / round_no / group_no / word_count / status / finish_time |
| `study_group_word` | 组内槽位 | group_id / word_id / position / status |
| `user_word` | 用户 × 词的状态（收藏 / 熟词 / 出现次数） | is_favorite / is_known / seen_count / first_wordbook_id |
| `feedback` | 意见反馈 | user_id / content / contact / status / reply / reply_time |
| `import_log` | Excel 导入记录 | wordbook_id / file_name / total_rows / success_rows / fail_rows / status / error_file |
| `announcement` | 公告 | title / content / status / publish_time |
| `word_audio` | 单词音频索引 | word_id / accent / voice / object_key / checksum / model_version / status |

关系链路：`wordbook —< wordbook_word >— word`；用户通过 `user_wordbook` 选书，`study_group —< study_group_word` 记录每组出过哪些词；`user_word` 是「用户 × 词」的唯一状态表，收藏夹与熟词本都是它的视图，不单独建表。`word_audio` 按 `word_id + accent + voice` 索引音频文件，音频文件本身存服务器磁盘，不存 BLOB。

**几个刻意的取舍：**

- **不建收藏夹表 / 熟词本表**：`user_word.is_favorite` / `is_known` 两个标志位就够，移出只是改回 0。
- **不建统计表**：进度与计数实时算，个人量级下正确性优先。
- **多词性不拆表**：`word.meaning` 用换行分隔，一行一个词性（`v. 放弃；遗弃` / `n. 放任`），`word.pos` 只留首个词性做兼容。后端 `WordVo` 把它解析成 `senses: [{pos, meaning}]` 再返回，前端只渲染不解析。
- **逻辑删除**：`app_user` / `wordbook` / `word` 带 `deleted` 字段，走 MyBatis-Plus `@TableLogic`；其余表物理删除。
- **组内空位是显式状态**：`study_group_word.status = 2` 表示「这个词标熟后无词可补」，前端据此渲染空卡片，而不是把整组压短。

## 8. 后端设计

### 8.1 分层

```
controller  → 接收请求、取 request attribute 里的 userId / adminId、返回 Result
service     → 业务规则与事务边界（@Transactional 只出现在 service 实现）
mapper      → 全部 extends BaseMapper<T>，无 XML、无自定义 SQL
domain      → 实体 + bo/（入参，带 @Valid）+ vo/（出参）
common      → Result / PageResult / BizException / GlobalExceptionHandler
config      → MyBatis-Plus 分页与逻辑删除、CORS、上传目录、token 签发、鉴权拦截
```

### 8.2 「零 SQL」约定

业务代码里不出现任何 SQL：不写 XML、不写 `@Select`、不拼子查询。所有查询用 `LambdaQueryWrapper`，复杂选词（取当前组 / 下一组 / 标熟换词）先把几组 ID 查出来，在 Java 里做集合差再随机取。分页用 `Page<T>` + `PaginationInnerInterceptor`。

单本词书最大约 5500 词，这套做法的内存与耗时都可接受，换来的是「改规则不用改 SQL」。

### 8.3 鉴权

- 登录成功签发 `token = base64url(type:userId:exp) + "." + base64url(HMAC-SHA256(payload))`，`type` 区分 `user` / `admin`，默认有效期 90 天（`APP_TOKEN_DAYS`），密钥取 `APP_TOKEN_SECRET`。
- 请求带 `Authorization: Bearer <token>`；`AuthInterceptor` 按 URI 前缀判定身份，把 `userId` 或 `adminId` 放进 request attribute。
- 完全无状态：重启、重新部署都不掉登录态；代价是服务端无法强制吊销（见 §16）。

### 8.4 统一响应与错误

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code = 0` 表示成功；业务异常由 `BizException` 抛出、`GlobalExceptionHandler` 兜成对应错误码；未登录固定 `401`。前端 axios 拦截器统一判定 `code !== 0`、弹提示、遇到 401 清 token 跳登录页。

## 9. 接口一览

在线文档：`http://47.95.66.191:8082/doc.html`（Knife4j，OpenAPI JSON 为 `/v3/api-docs`）。文档页公开访问，但所有业务接口仍校验 token；需要隐藏时可在 Nginx 对 `/doc.html`、`/v3/api-docs`、`/webjars/` 加 Basic Auth。

用户端（`Authorization` 为用户 token）：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/home` | 主页：当前词书、进度、收藏数、熟词数、连续天数 |
| GET | `/api/wordbooks` | 已上架词书列表 |
| GET | `/api/wordbooks/mine` | 我的书架 |
| POST | `/api/wordbooks/{id}/select` | 选书 |
| GET | `/api/study/current` | 取当前组（无则新建） |
| POST | `/api/study/group/{id}/next` | 完成本组、取下一组（幂等） |
| POST | `/api/study/group/{id}/known/{wordId}` | 标熟并即时换词 |
| POST | `/api/study/group/{id}/favorite/{wordId}` | 收藏 / 取消收藏 |
| GET · DELETE | `/api/favorites` · `/api/favorites/{wordId}` | 收藏夹 / 移出 |
| GET · DELETE | `/api/known` · `/api/known/{wordId}` | 熟词本 / 移出（移出后回到待背池） |
| GET | `/api/announcements` | 公告 |
| POST · GET | `/api/feedback` · `/api/feedback/mine` | 提交反馈 / 我的反馈与回复 |
| GET | `/api/app/version` | 最新版本号、下载地址、更新说明 |
| — | 学习组响应 | `words[].audioUrl` 为预生成 MP3 地址；没有音频时为空 |
| GET | `/actuator/health` | 健康检查（部署脚本据此判断存活） |

管理端（`Authorization` 为管理员 token，全部在 `/api/admin` 下）：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` · `/auth/password` | 登录 / 改密 |
| GET | `/dashboard` | 总览数据（KPI、词书、近 14 天活跃度、最近用户、最近导入、JVM 与运行时长） |
| GET/POST/PUT/DELETE | `/wordbooks` · `/wordbooks/{id}` · `/{id}/status` · `/{id}/words` | 词书 CRUD + 上下架 + 清空词 |
| GET/POST/PUT/DELETE | `/words` · `/words/{id}` | 词条 CRUD |
| POST | `/words/{id}/audio` | 上传/替换单词 MP3，更新 `word_audio` |
| POST · GET | `/import/wordbook` · `/import/logs` | Excel 导入 / 导入记录 |
| GET/POST/DELETE | `/users` · `/users/{id}/reset-password` · `/users/{id}` | 用户建号 / 重置密码 / 删除 |
| GET · POST | `/feedback` · `/feedback/{id}/reply` | 反馈列表 / 回复 |
| GET/POST/PUT/DELETE | `/announcements` · `/{id}` · `/{id}/status` | 公告 CRUD / 上下线 |

## 10. 前端结构

**用户端 `app/`**（路由 `createWebHistory('/')`，Vite 端口 5173）

| 路由 | 页面 | 说明 |
|---|---|---|
| `/login` | Login | 唯一入口，注册入口显式关闭 |
| `/` | Home | 词书进度、收藏/熟词入口、公告、连续天数 |
| `/study/:bookId` | Study | 背单词主界面 |
| `/favorites` · `/favorites/study` | Favorites / FavoriteStudy | 收藏夹与「背收藏夹」 |
| `/known` | Known | 熟词本（只读 + 移出） |
| `/me` | Me | 我的、反馈入口、退出 |

背单词页的交互细节值得单说：

- 每张卡分**单词区 / 释义区**两个点击区，各自可单独隐藏，状态纯前端、不落库。
- 底部保留**全局按钮**「隐藏单词 / 隐藏释义」，一键批量切换；之后仍可单卡点开某一张，单卡状态与全局按钮互不覆盖。
- 熟词、收藏按钮 `stopPropagation`，避免误触隐藏。
- 「下一组」两段式确认；退出时弹「要耍起了吗？」。
- 每个单词右侧有朗读按钮；加载一组后异步预加载 10 条 MP3，音频缺失时隐藏按钮。

状态管理只用一个 `stores/auth.ts`（token 存 `localStorage.app_token`），其余都是页面内状态。主题通过 `styles/tokens.css` 的 Design Token 统一（纸感编辑：暖白 `#f5f1e8` / 墨黑 `#1a1713` / 朱砂 `#b4432f`）。

**管理后台 `admin/`**（base `/admin/`，Vite 端口 5174，token 存 `admin_token`）：总览、词书、词条、导入、用户、反馈、公告七个页面，共用 `layouts/AdminLayout.vue` 的左侧 rail 布局，主题为深石墨 + 酸性青柠（`#08090b` / `#c8f560`）。

两端请求层都是单例 axios：`baseURL = '/api'`，请求拦截注入 Bearer token，响应拦截判定 `code !== 0` 并处理 401。

## 11. 本地运行

前置：JDK 17、Maven 3.9+、Node 20+、MySQL 8。

```bash
# 1) 建库（只需一次）
mysql -uroot -p < db/schema.sql      # 库名 shorthand

# 2) 后端，默认 8081
cd server && mvn -DskipTests package
MYSQL_USERNAME=root MYSQL_PASSWORD=你的密码 java -jar target/shorthand-server-0.1.0.jar

# 3) 管理后台，默认 5174（/api 已代理到 8081）
cd admin && npm install && npm run dev

# 4) 用户端，默认 5173（同样代理 /api）
cd app && npm install && npm run dev
```

冒烟测试（需后端已启动、已有管理员与至少一本上架词书）：

```bash
tools/smoke-test.sh     # BASE / ADMIN_USER / ADMIN_PASS / USER_NAME / USER_PASS 均可覆盖
```

后端可覆盖的环境变量：`SERVER_PORT`、`MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`MYSQL_POOL_SIZE`、`APP_UPLOAD_DIR`、`APP_AUDIO_DIR`、`APP_TOKEN_SECRET`、`APP_TOKEN_DAYS`、`APP_LATEST_VERSION`、`APP_DOWNLOAD_URL`、`APP_UPDATE_NOTES`。

## 12. 部署

部署形态是「一个 Java 进程 + 两份静态产物 + 一层反向代理」，没有容器编排，具体主机、端口、路径与账号不在此文交代：

- `server/` 打成可执行 jar，用 `java -jar` 或任意进程管理器起进程，端口取 `SERVER_PORT`（默认 8081）。
- `app/` 与 `admin/` 构建出的 `dist` 交给反向代理做静态站点：用户端挂在 `/`，管理后台挂在 `/admin/`（Vite `base` 已配好），`/api` 转发到 Java 进程，`/uploads` 直出上传目录。
- 部署物料（systemd 单元、反向代理站点、备份脚本、环境变量样例）不随仓库分发，按上面的形态自行准备。

CI/CD 是三条 GitHub Actions：

| 工作流 | 触发 | 做什么 |
|---|---|---|
| `deploy.yml` | push `main` | 构建 jar 与两份前端产物 → 上传 → 重启服务 → 健康检查，失败回滚到上一版；未配置部署密钥时整段跳过，只做构建 |
| `release.yml` | 推送 `v*` tag | 构建并签名 Android Release APK，发布到 GitHub Release |
| `android.yml` | `app/**` 变更 | 只出 debug APK，用于快速验证 |

部署主机与凭据一律走仓库 secrets，仓库里不放明文。

## 13. 安卓发布

用户端有可见改动就要发版 —— 版本号是用户能感知的东西（`app/src/version.ts` 的 `APP_VERSION`，与后端 `APP_LATEST_VERSION` 对齐，用于更新提示）。

发布说明是版本的一部分，先写 `.github/release-notes/vX.Y.Z.md`，再打 tag：

```bash
tools/release.sh v0.5.2        # 检查发布说明、打 tag 并推送
```

`tools/release.sh` 会先确认对应的 release note 存在；tag 推送触发 `.github/workflows/release.yml`：`npm run build` → `cap sync android` → 用 secrets 里的 keystore 签名 → `assembleRelease` → 上传 GitHub Release；Release 正文读取 `.github/release-notes/<tag>.md`，不再使用通用模板。APK 可直接覆盖安装。

打包时后端地址通过 `VITE_API_BASE` 注入；未注入时回落到 `app/src/api/http.ts` 里的默认值 —— 换环境请改这里或注入变量，别在代码里散落地址。

## 14. 词书导入

数据链路：**PDF/Excel 源文件 → `tools/` 脚本清洗 → 标准导入 Excel → 后台上传 → EasyExcel 读表入库**。

- `db/schema.sql` 不预置任何词条，词书完全由导入产生。
- 导入模板的「释义」单元格一行一个词性；`WordImportListener` 逐行读取，Service 写 `word` + `wordbook_word`，同时刷新 `wordbook.word_count` 并记 `import_log`（总行数 / 成功 / 失败 / 错误文件）。
- 脚本：`build_wordbooks.py`（四级 / 六级 / 词组 → 标准 Excel）、`build_gaokao_wordbooks.py`（OCR 版高考词书）、`check_phonetics.py`（音标比对与校对）。
- 词书内容不随仓库分发，需要用的人自备源文件跑脚本。

### 人工修正音频

自动生成的音频不进 Git；只把人工修正的 MP3 放进 `audio-overrides/`：

```bash
python tools/audio/make_override.py --word abandon --file ~/Downloads/abandon.mp3
git add audio-overrides
git commit -m "audio: 修正 abandon"
git push
```

推送后 `sync-audio-overrides` 工作流只上传本次变更的文件，并通过后台接口更新 `word_audio`。需要仓库 Secrets：`ADMIN_USERNAME`、`ADMIN_PASSWORD`。

## 15. 开发约定

改动这个仓库时请守住这些，它们比代码风格重要：

1. **业务代码零 SQL**：只用 `LambdaQueryWrapper` / `QueryWrapper`，不写 XML、不写 `@Select`；复杂选词在 Java 内存里算。
2. **不写自定义泛型基类**：Mapper 直接 `extends BaseMapper<Xxx>`，Service 用具体类型，不为「复用」抽象一层。
3. **不引入中间件**：没有 Redis、没有队列、没有缓存层。加这些之前先证明单机扛不住。
4. **事务只在 service 层**：`@Transactional` 不出现在 controller / mapper。
5. **入参出参分离**：controller 收 `bo/`（带 `@Valid`），返回 `vo/`，实体不出接口层。
6. **改规则要同步改文档**：动了学习模型就顺手改本文件 §6。
7. **不做清单**：注册、支付、社区、实时 TTS / 跟读评分、遗忘曲线、每日目标、复习队列 —— 想加先讨论产品定位，别直接写。单词朗读是预生成音频，不属于实时 TTS。

## 16. 已知限制

- **没有自动化测试**：`server/src/test` 不存在，前端也没有测试工程。唯一的回归手段是 `tools/smoke-test.sh` 与真机手测。这是当前最大的一块技术债。
- **token 无法服务端吊销**：无状态签名的代价。改 `APP_TOKEN_SECRET` 会让所有端一起掉线，这是唯一的「一键下线」手段。
- **选词在 JVM 内存里算**：每次取组要把本书全部 wordId + 已熟集合 + 本轮已用集合拉进内存。单书万级词以内没问题，再大就要换策略。
- **单实例，没有水平扩展设计**：单进程、小连接池；上传文件落本地磁盘，多实例之间不共享。
- **API 文档页公开**：`/doc.html` 只暴露接口结构，不提供免鉴权调用；业务接口仍需 token。生产环境若要隐藏，需在 Nginx 层加认证。
- **iOS 未维护**：`app/ios` 工程还在，但没有构建与发布链路。
- **词书需自备**：仓库不含任何词表数据，空库启动只有登录页。
- **音频需维护**：新增单词后要补跑 `tools/tts/generate_all.py --resume`，否则新词没有朗读按钮；当前已生成 6,073 条 MP3。

## 17. 许可

本仓库尚未声明开源许可证（默认保留所有权利）。如需引用、分发或二次使用，请先联系作者。
