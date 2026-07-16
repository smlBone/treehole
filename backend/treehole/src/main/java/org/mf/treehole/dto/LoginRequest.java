package org.mf.treehole.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;  // 前端加密后的密码
}
