package org.mf.treehole.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.*;
import org.mf.treehole.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody SendCodeRequest req) {
        return authService.sendCode(req);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        return authService.logout(request.getHeader("Authorization"));
    }
}
