package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemMessage {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
