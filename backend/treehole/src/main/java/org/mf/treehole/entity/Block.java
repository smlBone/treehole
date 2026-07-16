package org.mf.treehole.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Block {
    private Long id;
    private Long blockerId;
    private Long blockedId;
    private LocalDateTime createdAt;
}
