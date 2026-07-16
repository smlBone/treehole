package org.mf.treehole.dto;

import lombok.Data;

@Data
public class SendCodeRequest {
    private String email;
    private String purpose;  // REGISTER / RESET
}
