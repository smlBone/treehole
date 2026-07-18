package org.mf.treehole.service;

import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.mf.treehole.common.Constants;
import org.mf.treehole.common.Result;
import org.mf.treehole.dto.*;
import org.mf.treehole.entity.User;
import org.mf.treehole.mapper.RowMappers;
import org.mf.treehole.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Random;

@Service
public class AuthService {

    @Resource
    private JdbcTemplate jdbc;

    @Resource
    private StringRedisTemplate redis;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private JwtUtil jwtUtil;

    private static final Random random = new Random();

    private static final String FROM_EMAIL = "3184465467@qq.com";

    public Result<Void> sendCode(SendCodeRequest req) {
        String email = req.getEmail();
        String purpose = req.getPurpose() != null ? req.getPurpose() : "REGISTER";

        if ("REGISTER".equals(purpose)) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
            if (count != null && count > 0) {
                return Result.error(400, "该邮箱已注册");
            }
        } else if ("RESET".equals(purpose)) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
            if (count == null || count == 0) {
                return Result.error(400, "该邮箱未注册");
            }
        }

        // 限流：60秒内不能重复发送
        String rateLimitKey = "rate_limit:" + email;
        if (Boolean.TRUE.equals(redis.hasKey(rateLimitKey))) {
            return Result.error(429, "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", random.nextInt(1000000));
        String codeKey = Constants.REDIS_VERIFY_CODE + email;
        redis.opsForValue().set(codeKey, code, Duration.ofMinutes(5));
        redis.opsForValue().set(rateLimitKey, "1", Duration.ofSeconds(60));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);

            helper.setTo(email);
            helper.setSubject("【校园树洞】验证码");

            helper.setText("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #333; text-align: center;">验证码</h2>
                    <p style="font-size: 16px; color: #555;">您的验证码是：</p>
                    <div style="background: #f5f5f5; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; color: rgb(44, 219, 56); letter-spacing: 4px; border-radius: 5px;">
                        %s
                    </div>
                    <p style="font-size: 14px; color: #555; margin-top: 20px;">有效期5分钟，请勿泄露给他人。如非本人操作，请忽略此邮件。</p>
                    <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
                    <p style="font-size: 12px; color: #aaa;">此邮件由系统自动发送，请勿回复</p>
                </div>
                """.formatted(code), true);

            mailSender.send(message);

        } catch (Exception e) {
            return Result.error(500, "邮件发送失败");
        }
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom(FROM_EMAIL);
//        message.setTo(email);
//        message.setSubject("RESET".equals(purpose) ? "校园树洞 - 重置密码验证码" : "校园树洞 - 注册验证码");
//        message.setText("您的验证码是：" + code + "，有效期5分钟。如非本人操作，请忽略此邮件。");
//        mailSender.send(message);

        return Result.success();
    }

    public Result<String> register(RegisterRequest req) {
        String codeKey = Constants.REDIS_VERIFY_CODE + req.getEmail();
        String storedCode = redis.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, req.getEmail());
        if (count != null && count > 0) {
            return Result.error(400, "该邮箱已注册");
        }

        jdbc.update("INSERT INTO users (email, password, nickname, role, credit_score, status) VALUES (?, ?, ?, 'USER', 100, 'ACTIVE')",
                req.getEmail(), req.getPassword(), req.getNickname());
        redis.delete(codeKey);

        return Result.success("注册成功", null);
    }

    public Result<String> login(LoginRequest req) {
        User user = findByEmail(req.getEmail());
        if (user == null) {
            return Result.error(400, "邮箱或密码错误");
        }
        if (!user.getPassword().equals(req.getPassword())) {
            return Result.error(400, "邮箱或密码错误");
        }

        // 每日首次登录加1信誉分
        checkDailyLogin(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return Result.success("登录成功", token);
    }

    public Result<Void> resetPassword(ResetPasswordRequest req) {
        String codeKey = Constants.REDIS_VERIFY_CODE + req.getEmail();
        String storedCode = redis.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        jdbc.update("UPDATE users SET password = ? WHERE email = ?", req.getNewPassword(), req.getEmail());
        redis.delete(codeKey);
        return Result.success("密码重置成功", null);
    }

    public Result<Void> logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            redis.opsForValue().set(Constants.REDIS_JWT_BLACKLIST + token, "1", Duration.ofMillis(86400000));
        }
        return Result.success();
    }

    private void checkDailyLogin(User user) {
        if (user.getDailyLoginChecked() == null || user.getDailyLoginChecked() == 0) {
            int newScore = Math.min(100, user.getCreditScore() + 1);
            jdbc.update("UPDATE users SET credit_score = ?, daily_login_checked = 1 WHERE id = ?", newScore, user.getId());
        }
    }

    public User findByEmail(String email) {
        List<User> list = jdbc.query("SELECT * FROM users WHERE email = ?", RowMappers.USER, email);
        return list.isEmpty() ? null : list.getFirst();
    }

    public User findById(Long id) {
        List<User> list = jdbc.query("SELECT * FROM users WHERE id = ?", RowMappers.USER, id);
        return list.isEmpty() ? null : list.getFirst();
    }
}
