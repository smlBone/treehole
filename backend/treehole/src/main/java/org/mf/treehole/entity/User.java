package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private String bio;
    private String role;
    private Integer creditScore;
    private String status;
    private Integer likeReceivedCount;
    private Integer dailyLoginChecked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非数据库字段
    private Boolean isFollowing;  // 当前用户是否关注了此人
    private Boolean isBlocked;    // 当前用户是否拉黑了此人
    private Integer postCount;    // 发帖数
    private Integer followingCount;
    private Integer followerCount;
}
