package org.mf.treehole.service;

import jakarta.annotation.Resource;
import org.mf.treehole.common.*;
import org.mf.treehole.dto.CreatePostRequest;
import org.mf.treehole.entity.Post;
import org.mf.treehole.entity.User;
import org.mf.treehole.mapper.RowMappers;
import org.mf.treehole.util.DeepSeekChat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Resource
    private JdbcTemplate jdbc;

    @Resource
    private UserService userService;

    public Result<Post> createPost(CreatePostRequest req) {
        Long userId = UserContext.getUserId();
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 检查封禁状态
        if (Constants.STATUS_BANNED.equals(user.getStatus())) {
            return Result.error(403, "账号已被封禁，无法发帖");
        }

        // 验证板块
        String board = req.getBoard();
        if (!Constants.BOARD_WORLD.equals(board) && !Constants.BOARD_SECRET.equals(board)) {
            return Result.error(400, "无效的板块");
        }

        // 验证图片数量
        if (req.getImages() != null && req.getImages().size() > 9) {
            return Result.error(400, "最多只能上传9张图片");
        }

        // 确定帖子状态：信誉分>=90直接发布，60-89需审核
        String status;
        if (user.getCreditScore() >= Constants.CREDIT_DIRECT_POST) {
            status = Constants.POST_PUBLISHED;
        } else if (user.getCreditScore() >= Constants.CREDIT_NEED_REVIEW) {
            status = Constants.POST_PENDING;
        } else {
            return Result.error(403, "账号已被封禁，无法发帖");
        }

        String imagesJson = req.getImages() != null && !req.getImages().isEmpty()
                ? RowMappers.toJson(req.getImages()) : null;

        jdbc.update("""
                INSERT INTO posts (user_id, board, content, images, status, robot_responded)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                userId, board, req.getContent(), imagesJson, status,
                Boolean.TRUE.equals(req.getCallRobot()) && Constants.BOARD_SECRET.equals(board));

        Long postId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 如果是秘密树洞且调用了小树灵，异步获取AI回复
        if (Boolean.TRUE.equals(req.getCallRobot()) && Constants.BOARD_SECRET.equals(board)) {
            callRobotAsync(postId, req.getContent());
        }

        Post post = findById(postId);
        return Result.success("发帖成功" + (Constants.POST_PENDING.equals(status) ? "，帖子正在审核中" : ""), post);
    }

    public Result<PageResult<Post>> listPosts(String board, int page, int size) {
        Long currentUserId = UserContext.getUserId();

        // 获取被当前用户拉黑的用户ID列表
        List<Long> blockedUserIds = jdbc.queryForList(
                "SELECT blocked_id FROM blocks WHERE blocker_id = ?", Long.class, currentUserId);

        StringBuilder sql = new StringBuilder("""
                SELECT p.*, u.nickname AS author_nickname, u.avatar AS author_avatar, u.role AS author_role
                FROM posts p
                INNER JOIN users u ON p.user_id = u.id
                WHERE p.status = 'PUBLISHED' AND p.board = ?
                """);
        var params = new java.util.ArrayList<>();
        params.add(board);

        // 世界树洞：过滤掉被拉黑用户的帖子
        if (Constants.BOARD_WORLD.equals(board) && !blockedUserIds.isEmpty()) {
            sql.append(" AND p.user_id NOT IN (");
            for (int i = 0; i < blockedUserIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
                params.add(blockedUserIds.get(i));
            }
            sql.append(") ");
        }

        sql.append(" ORDER BY p.created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        List<Post> posts = jdbc.query(sql.toString(), (rs, rowNum) -> {
            Post p = RowMappers.POST.mapRow(rs, rowNum);
            p.setAuthorNickname(rs.getString("author_nickname"));
            p.setAuthorAvatar(rs.getString("author_avatar"));
            return p;
        }, params.toArray());

        // 秘密树洞：隐藏作者信息
        if (Constants.BOARD_SECRET.equals(board)) {
            posts.forEach(p -> {
                p.setUserId(null);
                p.setAuthorNickname("匿名用户");
                p.setAuthorAvatar(null);
            });
        }

        // 填充当前用户的互动状态
        posts.forEach(p -> {
            p.setIsAuthor(p.getUserId() != null && p.getUserId().equals(currentUserId));
            p.setIsLiked(hasLiked(currentUserId, Constants.TARGET_POST, p.getId()));
            p.setHasReported(hasReported(currentUserId, Constants.TARGET_POST, p.getId()));
        });

        String countSql = "SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED' AND board = ?";
        Long total = jdbc.queryForObject(countSql, Long.class, board);

        return Result.success(PageResult.of(posts, total != null ? total : 0, page, size));
    }

    public Result<Post> getPostDetail(Long postId) {
        Long currentUserId = UserContext.getUserId();
        Post post = findById(postId);
        if (post == null || Constants.POST_DELETED.equals(post.getStatus())) {
            return Result.error(404, "帖子不存在");
        }

        // 非作者/管理员不能查看待审核/拒绝的帖子
        if (Constants.POST_PENDING.equals(post.getStatus()) || Constants.POST_REJECTED.equals(post.getStatus())) {
            if (!post.getUserId().equals(currentUserId) && !UserContext.isAdmin()) {
                return Result.error(403, "帖子不可见");
            }
        }

        // 获取作者信息
        var user = userService.findById(post.getUserId());
        if (user != null) {
            post.setAuthorNickname(user.getNickname());
            post.setAuthorAvatar(user.getAvatar());
        }

        // 秘密树洞：隐藏作者信息（但保留小树灵的回复）
        if (Constants.BOARD_SECRET.equals(post.getBoard())) {
            if (user != null && !Constants.ROLE_SPECIAL.equals(user.getRole())) {
                post.setUserId(null);
                post.setAuthorNickname("匿名用户");
                post.setAuthorAvatar(null);
            }
        }

        post.setIsAuthor(post.getUserId() != null && post.getUserId().equals(currentUserId));
        post.setIsLiked(hasLiked(currentUserId, Constants.TARGET_POST, postId));
        post.setHasReported(hasReported(currentUserId, Constants.TARGET_POST, postId));

        // 记录浏览历史
        if (currentUserId != null) {
            try {
                jdbc.update("INSERT INTO browsing_history (user_id, post_id) VALUES (?, ?)", currentUserId, postId);
            } catch (Exception ignored) {}
        }

        return Result.success(post);
    }

    public Result<Void> deletePost(Long postId) {
        Long currentUserId = UserContext.getUserId();
        Post post = findById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }
        if (!post.getUserId().equals(currentUserId) && !UserContext.isAdmin()) {
            return Result.error(403, "无权删除此帖子");
        }
        jdbc.update("UPDATE posts SET status = 'DELETED' WHERE id = ?", postId);
        return Result.success();
    }

    public Result<Void> sharePost(Long postId) {
        jdbc.update("UPDATE posts SET share_count = share_count + 1 WHERE id = ?", postId);
        return Result.success();
    }

    public Result<PageResult<Post>> getUserPosts(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Post> posts = jdbc.query("""
                SELECT p.*, u.nickname AS author_nickname, u.avatar AS author_avatar
                FROM posts p
                INNER JOIN users u ON p.user_id = u.id
                WHERE p.user_id = ? AND p.status = 'PUBLISHED'
                ORDER BY p.created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            Post p = RowMappers.POST.mapRow(rs, rowNum);
            p.setAuthorNickname(rs.getString("author_nickname"));
            p.setAuthorAvatar(rs.getString("author_avatar"));
            return p;
        }, userId, size, offset);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE user_id = ? AND status = 'PUBLISHED'",
                Long.class, userId);

        return Result.success(PageResult.of(posts, total != null ? total : 0, page, size));
    }

    public Result<PageResult<Post>> getBrowsingHistory(int page, int size) {
        Long currentUserId = UserContext.getUserId();
        int offset = (page - 1) * size;
        List<Post> posts = jdbc.query("""
                SELECT p.*, u.nickname AS author_nickname, u.avatar AS author_avatar
                FROM browsing_history bh
                INNER JOIN posts p ON bh.post_id = p.id
                INNER JOIN users u ON p.user_id = u.id
                WHERE bh.user_id = ? AND p.status = 'PUBLISHED'
                ORDER BY bh.created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            Post p = RowMappers.POST.mapRow(rs, rowNum);
            p.setAuthorNickname(rs.getString("author_nickname"));
            p.setAuthorAvatar(rs.getString("author_avatar"));
            return p;
        }, currentUserId, size, offset);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM browsing_history WHERE user_id = ?",
                Long.class, currentUserId);

        return Result.success(PageResult.of(posts, total != null ? total : 0, page, size));
    }

    public Result<PageResult<Post>> getPendingPosts(int page, int size) {
        if (!UserContext.isReviewer()) {
            return Result.error(403, "无权操作");
        }
        int offset = (page - 1) * size;
        List<Post> posts = jdbc.query("""
                SELECT p.*, u.nickname AS author_nickname, u.avatar AS author_avatar
                FROM posts p
                INNER JOIN users u ON p.user_id = u.id
                WHERE p.status = 'PENDING'
                ORDER BY p.created_at
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            Post p = RowMappers.POST.mapRow(rs, rowNum);
            p.setAuthorNickname(rs.getString("author_nickname"));
            p.setAuthorAvatar(rs.getString("author_avatar"));
            return p;
        }, size, offset);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE status = 'PENDING'", Long.class);

        return Result.success(PageResult.of(posts, total != null ? total : 0, page, size));
    }

    public Result<Void> reviewPost(Long postId, String action) {
        if (!UserContext.isReviewer()) {
            return Result.error(403, "无权操作");
        }
        String status = "approve".equals(action) ? Constants.POST_PUBLISHED : Constants.POST_REJECTED;
        jdbc.update("UPDATE posts SET status = ? WHERE id = ?", status, postId);
        return Result.success();
    }

    public Post findById(Long postId) {
        var list = jdbc.query("SELECT * FROM posts WHERE id = ?", RowMappers.POST, postId);
        return list.isEmpty() ? null : list.getFirst();
    }

    private boolean hasLiked(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE user_id = ? AND target_type = ? AND target_id = ?",
                Integer.class, userId, targetType, targetId);
        return count != null && count > 0;
    }

    private boolean hasReported(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE reporter_id = ? AND target_type = ? AND target_id = ?",
                Integer.class, userId, targetType, targetId);
        return count != null && count > 0;
    }

    private void callRobotAsync(Long postId, String content) {
        DeepSeekChat.TreeholeRobot(content, responseBody -> {
            String result = responseBody.getResult();
            if (!"error".equals(result) && !"null".equals(result)) {
                jdbc.update("UPDATE posts SET robot_response = ? WHERE id = ?", result, postId);
            }
        });
    }
}
