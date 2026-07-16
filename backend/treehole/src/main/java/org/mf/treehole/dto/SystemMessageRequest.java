package org.mf.treehole.dto;

import lombok.Data;

@Data
public class SystemMessageRequest {
    private Long userId;   // null表示发给所有人
    private String title;
    private String content;
}
