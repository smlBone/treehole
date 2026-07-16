# 校园树洞

文档是AI写的（

## 项目概述

基于 Spring Boot 4.1 + Vue 3 全栈开发的"校园树洞"论坛网站，包含世界树洞和秘密树洞两大板块，支持匿名倾诉、AI慰藉、私信、信誉审核等完整功能。

## 技术栈

### 后端
- **框架**: Spring Boot 4.1 (Java 25)
- **数据库**: MySQL + Spring JDBC (JdbcTemplate)
- **缓存**: Redis (验证码、JWT黑名单)
- **认证**: JWT (jjwt 0.12.6)
- **邮件**: Spring Boot Mail (QQ邮箱SMTP)
- **AI**: DeepSeek API (小树灵)

### 前端
- **框架**: Vue 3.5 (Composition API, `<script setup>`)
- **路由**: Vue Router 5
- **状态管理**: Pinia 3
- **HTTP**: Axios
- **构建工具**: Vite 8
- **密码加密**: Web Crypto API (SHA-256)

## 功能模块

### 1. 用户系统
- 注册登录（邮箱验证码验证）
- 前端密码加密（SHA-256 + Salt）
- JWT 身份认证
- 4种角色：普通用户、特殊用户(小树灵)、审核员、管理员
- 个人主页（修改信息、查看发帖/浏览记录/获赞数）
- 关注/拉黑功能
- 小树灵特殊用户（秘密树洞中不隐藏信息，主页不显示发帖记录等）

### 2. 树洞/帖子板块
- 世界树洞（显示发帖人信息）
- 秘密树洞（完全匿名）
- 秘密树洞@小树灵求安慰（AI异步回复）
- 发帖人评论区标识（"作者"标签）
- 最多9张图片上传
- 点赞、评论、转发（复制URL）、举报、删除
- 举报类型选择（色情低俗、涉政敏感等6种）+ 理由填写
- 未登录用户可浏览但不可互动

### 3. 私聊/消息系统
- 关注后可发私信
- 未收到回复前只能发1条消息
- 拉黑后双方无法发消息，且世界树洞不显示对方帖子
- 系统消息（举报受理反馈）
- 管理员可向全部/指定用户发送系统消息

### 4. 信誉分/审核系统
- 信誉分 0-100，初始100，仅本人和管理员可见
- ≥90直接发帖 | 60-89需审核 | <60封禁
- 每日首次登录+1，每10次被赞+1，被举报成立管理员决定扣减
- 封禁账号仅能浏览

### 5. 其他
- 不使用 alert/confirm（使用 Toast 通知组件）
- 主题切换：跟随系统、深色、浅色（CSS变量实现）

## 项目结构

```
treeholeDemo2/
├── 文档.md                          # 需求文档
├── overview.md                      # 本文件
├── backend/treehole/                # 后端
│   ├── pom.xml                      # Maven依赖
│   ├── src/main/resources/
│   │   ├── application.yaml         # 配置文件
│   │   └── db/schema.sql            # 数据库建表SQL
│   └── src/main/java/org/mf/treehole/
│       ├── TreeholeApplication.java # 启动类
│       ├── common/                  # 通用类（Result, Constants, UserContext等）
│       ├── config/                  # 配置（WebConfig, Redis, 定时任务, 全局异常）
│       ├── interceptor/             # JWT拦截器
│       ├── util/                    # 工具（JWT, DeepSeek AI）
│       ├── entity/                  # 实体类
│       ├── dto/                     # 数据传输对象
│       ├── mapper/                  # RowMapper
│       ├── service/                 # 业务逻辑层
│       └── controller/              # REST控制器
└── frontend/treehole/               # 前端
    ├── package.json
    ├── vite.config.js               # Vite配置（含API代理）
    └── src/
        ├── main.js                  # 入口
        ├── App.vue                  # 根组件
        ├── assets/styles/           # 全局样式 + 主题变量
        ├── router/                  # 路由配置
        ├── stores/                  # Pinia状态管理
        ├── api/                     # Axios API封装
        ├── utils/                   # 工具函数
        ├── components/              # 公共组件
        │   ├── layout/              # 布局组件
        │   ├── common/              # 通用组件
        │   └── post/                # 帖子组件
        └── views/                   # 页面视图
            ├── auth/                # 登录/注册
            ├── WorldView.vue        # 世界树洞
            ├── SecretView.vue       # 秘密树洞
            ├── PostDetailView.vue   # 帖子详情
            ├── CreatePostView.vue   # 发帖
            ├── ProfileView.vue      # 用户主页
            ├── MessagesView.vue     # 消息
            ├── SettingsView.vue     # 设置
            ├── AdminView.vue        # 管理后台
            └── NotFoundView.vue     # 404
```

## 启动指南

### 1. 数据库
```bash
# 执行SQL建表
mysql -u root -p < backend/treehole/src/main/resources/db/schema.sql
```

### 2. 后端
```bash
# 设置环境变量
export DEEPSEEK_API_KEY=your_api_key

# 修改 application.yaml 中的数据库密码和邮箱配置

# 启动
cd backend/treehole
./mvnw spring-boot:run
```
后端运行在 `http://localhost:8080`

### 3. 前端
```bash
cd frontend/treehole
npm install
npm run dev
```
前端运行在 `http://localhost:5173`

## API 接口概览

| 模块 | 接口 | 方法 |
|------|------|------|
| 认证 | /api/auth/send-code | POST |
| 认证 | /api/auth/register | POST |
| 认证 | /api/auth/login | POST |
| 认证 | /api/auth/reset-password | POST |
| 认证 | /api/auth/logout | POST |
| 用户 | /api/users/me | GET |
| 用户 | /api/users/{id} | GET |
| 用户 | /api/users/profile | PUT |
| 用户 | /api/users/password | PUT |
| 用户 | /api/users/{id}/follow | POST/DELETE |
| 用户 | /api/users/{id}/block | POST/DELETE |
| 帖子 | /api/posts | POST |
| 帖子 | /api/posts/public/list | GET |
| 帖子 | /api/posts/public/{id} | GET |
| 帖子 | /api/posts/{id} | DELETE |
| 帖子 | /api/posts/{id}/share | POST |
| 帖子 | /api/posts/pending | GET |
| 帖子 | /api/posts/{id}/review | POST |
| 评论 | /api/comments | POST |
| 评论 | /api/comments/post/{postId} | GET |
| 评论 | /api/comments/{id} | DELETE |
| 点赞 | /api/likes/{type}/{id} | POST |
| 举报 | /api/reports | POST |
| 举报 | /api/reports/pending | GET |
| 举报 | /api/reports/handle | POST |
| 消息 | /api/messages | POST |
| 消息 | /api/messages/conversations | GET |
| 消息 | /api/messages/chat/{userId} | GET |
| 消息 | /api/messages/system | GET |
| 消息 | /api/messages/system/broadcast | POST |
| 文件 | /api/files/upload | POST |
