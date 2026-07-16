-- 校园树洞数据库
CREATE DATABASE IF NOT EXISTS treehole DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE treehole;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(512) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(512) DEFAULT NULL,
    bio TEXT DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER=普通用户, SPECIAL=特殊用户(小树灵), REVIEWER=审核员, ADMIN=管理员',
    credit_score INT NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE=正常, BANNED=封禁',
    like_received_count INT NOT NULL DEFAULT 0,
    daily_login_checked TINYINT NOT NULL DEFAULT 0 COMMENT '今日是否已首次登录加分',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 验证码表
CREATE TABLE IF NOT EXISTS verification_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    purpose VARCHAR(20) NOT NULL DEFAULT 'REGISTER' COMMENT 'REGISTER=注册, RESET=重置密码',
    expires_at DATETIME NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 帖子表
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    board VARCHAR(20) NOT NULL COMMENT 'WORLD=世界树洞, SECRET=秘密树洞',
    content TEXT NOT NULL,
    images JSON DEFAULT NULL,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    share_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PENDING=待审核, PUBLISHED=已发布, REJECTED=已拒绝, DELETED=已删除',
    robot_response TEXT DEFAULT NULL COMMENT '小树灵AI回复内容',
    robot_responded TINYINT NOT NULL DEFAULT 0 COMMENT '是否已@小树灵',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_board_status (board, status),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT DEFAULT NULL COMMENT '回复的评论ID, NULL为直接评论帖子',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PENDING, PUBLISHED, REJECTED, DELETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'POST=帖子, COMMENT=评论',
    target_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关注表
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followed_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, followed_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 拉黑表
CREATE TABLE IF NOT EXISTS blocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_block (blocker_id, blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 私聊消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender_receiver (sender_id, receiver_id),
    INDEX idx_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统消息表
CREATE TABLE IF NOT EXISTS system_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT 'NULL表示发给所有人',
    title VARCHAR(200) DEFAULT NULL,
    content TEXT NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 举报表
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'POST=帖子, COMMENT=评论',
    target_id BIGINT NOT NULL,
    report_type VARCHAR(30) NOT NULL COMMENT 'PORN=色情低俗, POLITICAL=涉政敏感, CYBERBULLY=引战网暴, FRAUD=涉嫌诈骗, RUMOR=传播谣言, OTHER=其它',
    reason TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING=待处理, APPROVED=举报成立, REJECTED=举报驳回',
    handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    penalty INT DEFAULT NULL COMMENT '扣除信誉分',
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 浏览记录表
CREATE TABLE IF NOT EXISTS browsing_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入小树灵特殊用户 (密码: 不会使用, 仅占位)
INSERT INTO users (email, password, nickname, avatar, bio, role, credit_score)
VALUES ('robot@treehole.local', 'robot_placeholder', '小树灵', NULL, '我是小树灵，专门在秘密树洞中为你带来慰藉的暖心AI 🌿', 'SPECIAL', 100)
ON DUPLICATE KEY UPDATE nickname = '小树灵';
