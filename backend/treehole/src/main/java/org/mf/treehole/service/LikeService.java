package org.mf.treehole.service;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Constants;
import org.mf.treehole.common.Result;
import org.mf.treehole.common.UserContext;
import org.mf.treehole.entity.Post;
import org.mf.treehole.entity.Comment;
import org.mf.treehole.mapper.RowMappers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LikeService {

    @Resource
    private JdbcTemplate jdbc;

    @Resource
    private UserService userService;

    public Result<Map<String, Object>> toggleLike(String targetType, Long targetId) {
        Long userId = UserContext.getUserId();
        var user = userService.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (Constants.STATUS_BANNED.equals(user.getStatus())) {
            return Result.error(403, "账号已被封禁");
        }

        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE user_id = ? AND target_type = ? AND target_id = ?",
                Integer.class, userId, targetType, targetId);

        boolean liked;
        if (existing != null && existing > 0) {
            // 取消点赞
            jdbc.update("DELETE FROM likes WHERE user_id = ? AND target_type = ? AND target_id = ?",
                    userId, targetType, targetId);
            updateLikeCount(targetType, targetId, -1);
            liked = false;
        } else {
            // 点赞
            jdbc.update("INSERT INTO likes (user_id, target_type, target_id) VALUES (?, ?, ?)",
                    userId, targetType, targetId);
            updateLikeCount(targetType, targetId, 1);
            liked = true;

            // 每10次被赞+1信誉分
            if (liked) {
                Long targetUserId = getTargetUserId(targetType, targetId);
                if (targetUserId != null && !targetUserId.equals(userId)) {
                    checkAndAddCreditForLikes(targetUserId);
                }
            }
        }

        Integer newCount = getCurrentLikeCount(targetType, targetId);
        return Result.success(Map.of("liked", liked, "likeCount", newCount));
    }

    private void updateLikeCount(String targetType, Long targetId, int delta) {
        if (Constants.TARGET_POST.equals(targetType)) {
            jdbc.update("UPDATE posts SET like_count = like_count + ? WHERE id = ?", delta, targetId);
        } else if (Constants.TARGET_COMMENT.equals(targetType)) {
            // 评论没有单独的like_count字段, 可通过查询likes表获取
        }
    }

    private Long getTargetUserId(String targetType, Long targetId) {
        if (Constants.TARGET_POST.equals(targetType)) {
            var list = jdbc.query("SELECT user_id FROM posts WHERE id = ?", (rs, rowNum) -> rs.getLong("user_id"), targetId);
            return list.isEmpty() ? null : list.get(0);
        } else if (Constants.TARGET_COMMENT.equals(targetType)) {
            var list = jdbc.query("SELECT user_id FROM comments WHERE id = ?", (rs, rowNum) -> rs.getLong("user_id"), targetId);
            return list.isEmpty() ? null : list.get(0);
        }
        return null;
    }

    private void checkAndAddCreditForLikes(Long targetUserId) {
        var user = userService.findById(targetUserId);
        if (user == null) return;

        Integer totalLikes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes l INNER JOIN posts p ON l.target_id = p.id AND l.target_type = 'POST' WHERE p.user_id = ?",
                Integer.class, targetUserId);
        if (totalLikes == null) totalLikes = 0;

        Integer commentLikes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes l INNER JOIN comments c ON l.target_id = c.id AND l.target_type = 'COMMENT' WHERE c.user_id = ?",
                Integer.class, targetUserId);
        if (commentLikes == null) commentLikes = 0;

        int totalLiked = totalLikes + commentLikes;
        int newScore = Math.min(100, user.getCreditScore() + (totalLiked / 10 - user.getLikeReceivedCount() / 10));

        if (newScore > user.getCreditScore()) {
            jdbc.update("UPDATE users SET credit_score = ?, like_received_count = ? WHERE id = ?",
                    newScore, totalLiked, targetUserId);
        } else {
            jdbc.update("UPDATE users SET like_received_count = ? WHERE id = ?", totalLiked, targetUserId);
        }
    }

    private Integer getCurrentLikeCount(String targetType, Long targetId) {
        if (Constants.TARGET_POST.equals(targetType)) {
            Integer count = jdbc.queryForObject("SELECT like_count FROM posts WHERE id = ?", Integer.class, targetId);
            return count != null ? count : 0;
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE target_type = ? AND target_id = ?", Integer.class, targetType, targetId);
        return count != null ? count : 0;
    }
}
