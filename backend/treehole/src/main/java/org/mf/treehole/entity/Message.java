package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // 非数据库字段
    private String senderNickname;
    private String senderAvatar;
    private String receiverNickname;
    private String receiverAvatar;
}
