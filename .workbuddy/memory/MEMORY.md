# 校园树洞项目 - 长期记忆

## 项目概述
- **名称**: 校园树洞 (treehole)
- **技术栈**: Spring Boot 4.1 (Java 25) + Vue 3 + MySQL + Redis
- **位置**: D:\newJava\javaproject\treeholeDemo2

## 技术要点
- 后端使用 Spring JDBC (JdbcTemplate)，非 JPA/MyBatis
- Jackson 3 API (包名 tools.jackson，非 com.fasterxml.jackson)
- 前端使用 Vue 3 Composition API (`<script setup>`)，无 TypeScript
- 密码加密：前端 SHA-256 + Salt，后端直接存储哈希值
- JWT 认证：jjwt 0.12.6
- AI集成：DeepSeek API，小树灵角色用于秘密树洞安慰

## 数据库
- MySQL: jdbc:mysql://localhost:3306/treehole
- 建表SQL: backend/treehole/src/main/resources/db/schema.sql
- Redis: localhost:6379 (验证码、JWT黑名单)

## 关键配置
- 邮箱: 3184465467@qq.com (QQ SMTP 465)
- 后端端口: 8080
- 前端端口: 5173 (Vite proxy 转发 /api 和 /uploads)

## 4种用户角色
- USER: 普通用户
- SPECIAL: 小树灵 (AI, ID=1)
- REVIEWER: 审核员
- ADMIN: 管理员
