package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Report {
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reportType;
    private String reason;
    private String status;
    private Long handlerId;
    private Integer penalty;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    // 非数据库字段
    private String reporterNickname;
    private String targetContent;  // 被举报的内容摘要
    private String handlerNickname;
}
