package org.mf.treehole.dto;

import lombok.Data;

@Data
public class ReportRequest {
    private String targetType;   // POST / COMMENT
    private Long targetId;
    private String reportType;   // PORN / POLITICAL / CYBERBULLY / FRAUD / RUMOR / OTHER
    private String reason;       // 举报理由, OTHER时必填
}
