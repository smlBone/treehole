package org.mf.treehole.dto;

import lombok.Data;

@Data
public class CreateCommentRequest {
    private Long postId;
    private String content;
    private Long parentId;  // 回复某条评论, null为直接评论帖子
}
