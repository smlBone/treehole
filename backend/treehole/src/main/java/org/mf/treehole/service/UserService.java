package org.mf.treehole.service;

import jakarta.annotation.Resource;
import org.mf.treehole.common.Constants;
import org.mf.treehole.common.Result;
import org.mf.treehole.common.UserContext;
import org.mf.treehole.dto.ChangePasswordRequest;
import org.mf.treehole.dto.UpdateProfileRequest;
import org.mf.treehole.entity.User;
import org.mf.treehole.mapper.RowMappers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Resource
    private JdbcTemplate jdbc;

    public Result<User> getMyProfile() {
        Long userId = UserContext.getUserId();
        User user = findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        enrichUserStats(user, userId);
        return Result.success(user);
    }

    public Result<User> getUserProfile(Long targetId) {
        Long currentUserId = UserContext.getUserId();
        User user = findById(targetId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 小树灵的特殊处理：不显示发帖记录、浏览记录、获赞总数
        boolean isSpecial = Constants.ROLE_SPECIAL.equals(user.getRole());

        // 信誉分仅本人和管理员可见
        if (!targetId.equals(currentUserId) && !UserContext.isAdmin()) {
            user.setCreditScore(null);
        }
        user.setPassword(null);

        // 关注/拉黑状态
        user.setIsFollowing(isFollowing(currentUserId, targetId));
        user.setIsBlocked(isBlocked(currentUserId, targetId));

        if (!isSpecial || targetId.equals(currentUserId) || UserContext.isAdmin()) {
            enrichUserStats(user, currentUserId);
        }

        return Result.success(user);
    }

    public Result<User> updateProfile(UpdateProfileRequest req) {
        Long userId = UserContext.getUserId();
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        var params = new java.util.ArrayList<Object>();
        if (req.getNickname() != null) {
            sql.append("nickname = ?, ");
            params.add(req.getNickname());
        }
        if (req.getAvatar() != null) {
            sql.append("avatar = ?, ");
            params.add(req.getAvatar());
        }
        if (req.getBio() != null) {
            sql.append("bio = ?, ");
            params.add(req.getBio());
        }
        if (params.isEmpty()) {
            return Result.error(400, "没有需要更新的内容");
        }
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(userId);
        jdbc.update(sql.toString(), params.toArray());
        return Result.success(findById(userId));
    }

    public Result<Void> changePassword(ChangePasswordRequest req) {
        Long userId = UserContext.getUserId();
        User user = findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (!user.getPassword().equals(req.getOldPassword())) {
            return Result.error(400, "原密码错误");
        }
        jdbc.update("UPDATE users SET password = ? WHERE id = ?", req.getNewPassword(), userId);
        return Result.success("密码修改成功", null);
    }

    public Result<Void> follow(Long targetId) {
        Long userId = UserContext.getUserId();
        if (userId.equals(targetId)) {
            return Result.error(400, "不能关注自己");
        }
        try {
            jdbc.update("INSERT IGNORE INTO follows (follower_id, followed_id) VALUES (?, ?)", userId, targetId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(400, "关注失败");
        }
    }

    public Result<Void> unfollow(Long targetId) {
        Long userId = UserContext.getUserId();
        jdbc.update("DELETE FROM follows WHERE follower_id = ? AND followed_id = ?", userId, targetId);
        return Result.success();
    }

    public Result<Void> block(Long targetId) {
        Long userId = UserContext.getUserId();
        if (userId.equals(targetId)) {
            return Result.error(400, "不能拉黑自己");
        }
        jdbc.update("INSERT IGNORE INTO blocks (blocker_id, blocked_id) VALUES (?, ?)", userId, targetId);
        // 拉黑后自动取消关注
        jdbc.update("DELETE FROM follows WHERE (follower_id = ? AND followed_id = ?) OR (follower_id = ? AND followed_id = ?)",
                userId, targetId, targetId, userId);
        return Result.success();
    }

    public Result<Void> unblock(Long targetId) {
        Long userId = UserContext.getUserId();
        jdbc.update("DELETE FROM blocks WHERE blocker_id = ? AND blocked_id = ?", userId, targetId);
        return Result.success();
    }

    public Result<List<User>> getFollowingList(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<User> list = jdbc.query("""
                SELECT u.* FROM users u
                INNER JOIN follows f ON u.id = f.followed_id
                WHERE f.follower_id = ?
                ORDER BY f.created_at DESC
                LIMIT ? OFFSET ?
                """, RowMappers.USER, userId, size, offset);
        list.forEach(u -> {
            u.setPassword(null);
            u.setCreditScore(null);
        });
        return Result.success(list);
    }

    public Result<List<User>> getFollowerList(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<User> list = jdbc.query("""
                SELECT u.* FROM users u
                INNER JOIN follows f ON u.id = f.follower_id
                WHERE f.followed_id = ?
                ORDER BY f.created_at DESC
                LIMIT ? OFFSET ?
                """, RowMappers.USER, userId, size, offset);
        list.forEach(u -> {
            u.setPassword(null);
            u.setCreditScore(null);
        });
        return Result.success(list);
    }

    public User findById(Long id) {
        var list = jdbc.query("SELECT * FROM users WHERE id = ?", RowMappers.USER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    private void enrichUserStats(User user, Long currentUserId) {
        Integer postCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE user_id = ? AND status = 'PUBLISHED'",
                Integer.class, user.getId());
        user.setPostCount(postCount != null ? postCount : 0);

        Integer followingCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE follower_id = ?",
                Integer.class, user.getId());
        user.setFollowingCount(followingCount != null ? followingCount : 0);

        Integer followerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE followed_id = ?",
                Integer.class, user.getId());
        user.setFollowerCount(followerCount != null ? followerCount : 0);
    }

    public boolean isFollowing(Long followerId, Long followedId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE follower_id = ? AND followed_id = ?",
                Integer.class, followerId, followedId);
        return count != null && count > 0;
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blocks WHERE blocker_id = ? AND blocked_id = ?",
                Integer.class, blockerId, blockedId);
        return count != null && count > 0;
    }

    public boolean isEitherBlocked(Long userA, Long userB) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blocks WHERE (blocker_id = ? AND blocked_id = ?) OR (blocker_id = ? AND blocked_id = ?)",
                Integer.class, userA, userB, userB, userA);
        return count != null && count > 0;
    }
}
