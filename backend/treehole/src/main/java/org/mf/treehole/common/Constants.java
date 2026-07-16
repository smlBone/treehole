package org.mf.treehole.common;

public class Constants {
    // 角色
    public static final String ROLE_USER = "USER";
    public static final String ROLE_SPECIAL = "SPECIAL";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_ADMIN = "ADMIN";

    // 用户状态
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BANNED = "BANNED";

    // 帖子板块
    public static final String BOARD_WORLD = "WORLD";
    public static final String BOARD_SECRET = "SECRET";

    // 帖子状态
    public static final String POST_PENDING = "PENDING";
    public static final String POST_PUBLISHED = "PUBLISHED";
    public static final String POST_REJECTED = "REJECTED";
    public static final String POST_DELETED = "DELETED";

    // 举报类型
    public static final String REPORT_PORN = "PORN";
    public static final String REPORT_POLITICAL = "POLITICAL";
    public static final String REPORT_CYBERBULLY = "CYBERBULLY";
    public static final String REPORT_FRAUD = "FRAUD";
    public static final String REPORT_RUMOR = "RUMOR";
    public static final String REPORT_OTHER = "OTHER";

    // 举报状态
    public static final String REPORT_PENDING = "PENDING";
    public static final String REPORT_APPROVED = "APPROVED";
    public static final String REPORT_REJECTED = "REJECTED";

    // 点赞目标类型
    public static final String TARGET_POST = "POST";
    public static final String TARGET_COMMENT = "COMMENT";

    // Redis key前缀
    public static final String REDIS_VERIFY_CODE = "verify_code:";
    public static final String REDIS_JWT_BLACKLIST = "jwt_blacklist:";
    public static final String REDIS_DAILY_LOGIN = "daily_login:";

    // 小树灵用户ID (数据库自动插入, 通常为1)
    public static final long ROBOT_USER_ID = 1L;

    // 信誉分阈值
    public static final int CREDIT_DIRECT_POST = 90;
    public static final int CREDIT_NEED_REVIEW = 60;
}
