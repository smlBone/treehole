package org.mf.treehole.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String oldPassword;  // 前端加密后
    private String newPassword;  // 前端加密后
}
