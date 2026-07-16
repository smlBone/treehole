package org.mf.treehole.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;  // 前端加密后的密码
    private String nickname;
    private String code;      // 邮箱验证码
}
