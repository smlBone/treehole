package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Long parentId;
    private String status;
    private LocalDateTime createdAt;

    // 非数据库字段
    private String authorNickname;
    private String authorAvatar;
    private String authorRole;
    private Boolean isPostAuthor;  // 是否是帖子作者
    private Boolean isLiked;
    private String replyToNickname; // 回复对象昵称
}
