package org.mf.treehole.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePostRequest {
    private String board;      // WORLD / SECRET
    private String content;
    private List<String> images;
    private Boolean callRobot; // 秘密树洞是否@小树灵
}
