package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Post {
    private Long id;
    private Long userId;
    private String board;
    private String content;
    private List<String> images;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private String status;
    private String robotResponse;
    private Boolean robotResponded;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非数据库字段
    private String authorNickname;
    private String authorAvatar;
    private Boolean isLiked;      // 当前用户是否已赞
    private Boolean isAuthor;     // 当前用户是否是作者
    private Boolean hasReported;  // 当前用户是否已举报
}
