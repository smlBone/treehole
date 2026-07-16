package org.mf.treehole.dto;

import lombok.Data;

@Data
public class HandleReportRequest {
    private Long reportId;
    private String status;    // APPROVED / REJECTED
    private Integer penalty;  // 扣除信誉分(仅APPROVED时)
}
