package org.mf.treehole.service;

import jakarta.annotation.Resource;
import org.mf.treehole.common.*;
import org.mf.treehole.dto.CreateCommentRequest;
import org.mf.treehole.entity.Comment;
import org.mf.treehole.entity.Post;
import org.mf.treehole.mapper.RowMappers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Resource
    private JdbcTemplate jdbc;

    @Resource
    private UserService userService;

    @Resource
    private PostService postService;

    public Result<Comment> createComment(CreateCommentRequest req) {
        Long userId = UserContext.getUserId();
        var user = userService.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (Constants.STATUS_BANNED.equals(user.getStatus())) {
            return Result.error(403, "账号已被封禁");
        }

        Post post = postService.findById(req.getPostId());
        if (post == null || !Constants.POST_PUBLISHED.equals(post.getStatus())) {
            return Result.error(404, "帖子不存在或不可评论");
        }

        // 确定评论状态
        String status;
        if (user.getCreditScore() >= Constants.CREDIT_DIRECT_POST) {
            status = Constants.POST_PUBLISHED;
        } else {
            status = Constants.POST_PENDING;
        }

        jdbc.update("""
                INSERT INTO comments (post_id, user_id, content, parent_id, status)
                VALUES (?, ?, ?, ?, ?)
                """, req.getPostId(), userId, req.getContent(), req.getParentId(), status);

        Long commentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 更新帖子评论数
        if (Constants.POST_PUBLISHED.equals(status)) {
            jdbc.update("UPDATE posts SET comment_count = comment_count + 1 WHERE id = ?", req.getPostId());
        }

        Comment comment = findCommentById(commentId);
        if (comment != null) {
            comment.setAuthorNickname(user.getNickname());
            comment.setAuthorAvatar(user.getAvatar());
            comment.setAuthorRole(user.getRole());
            comment.setIsPostAuthor(post.getUserId().equals(userId));

            // 如果有parent_id，获取被回复者昵称
            if (req.getParentId() != null) {
                Comment parent = findCommentById(req.getParentId());
                if (parent != null) {
                    var parentUser = userService.findById(parent.getUserId());
                    if (parentUser != null) {
                        comment.setReplyToNickname(parentUser.getNickname());
                    }
                }
            }
        }

        return Result.success(status.equals(Constants.POST_PENDING) ? "评论正在审核中" : "评论成功", comment);
    }

    public Result<List<Comment>> listComments(Long postId) {
        Long currentUserId = UserContext.getUserId();
        List<Comment> comments = jdbc.query("""
                SELECT c.*, u.nickname AS author_nickname, u.avatar AS author_avatar, u.role AS author_role
                FROM comments c
                INNER JOIN users u ON c.user_id = u.id
                WHERE c.post_id = ? AND c.status = 'PUBLISHED'
                ORDER BY c.created_at
                """, (rs, rowNum) -> {
            Comment c = RowMappers.COMMENT.mapRow(rs, rowNum);
            c.setAuthorNickname(rs.getString("author_nickname"));
            c.setAuthorAvatar(rs.getString("author_avatar"));
            c.setAuthorRole(rs.getString("author_role"));
            return c;
        }, postId);

        // 获取帖子作者ID
        Post post = postService.findById(postId);
        Long postAuthorId = post != null ? post.getUserId() : null;

        // 是否是秘密树洞
        boolean isSecret = post != null && Constants.BOARD_SECRET.equals(post.getBoard());

        comments.forEach(c -> {
            c.setIsPostAuthor(postAuthorId != null && postAuthorId.equals(c.getUserId()));
            c.setIsLiked(hasLiked(currentUserId, Constants.TARGET_COMMENT, c.getId()));

            // 秘密树洞中隐藏评论者身份（小树灵除外）
            if (isSecret && !Constants.ROLE_SPECIAL.equals(c.getAuthorRole())) {
                c.setUserId(null);
                c.setAuthorNickname("匿名用户");
                c.setAuthorAvatar(null);
            }

            // 获取回复对象昵称
            if (c.getParentId() != null) {
                Comment parent = findCommentById(c.getParentId());
                if (parent != null) {
                    var parentUser = userService.findById(parent.getUserId());
                    if (parentUser != null) {
                        String replyName = isSecret && !Constants.ROLE_SPECIAL.equals(parentUser.getRole())
                                ? "匿名用户" : parentUser.getNickname();
                        c.setReplyToNickname(replyName);
                    }
                }
            }
        });

        return Result.success(comments);
    }

    public Result<Void> deleteComment(Long commentId) {
        Long currentUserId = UserContext.getUserId();
        Comment comment = findCommentById(commentId);
        if (comment == null) {
            return Result.error(404, "评论不存在");
        }

        // 检查权限：评论作者、帖子作者或管理员可以删除
        Post post = postService.findById(comment.getPostId());
        boolean canDelete = comment.getUserId().equals(currentUserId)
                || (post != null && post.getUserId().equals(currentUserId))
                || UserContext.isAdmin();
        if (!canDelete) {
            return Result.error(403, "无权删除此评论");
        }

        jdbc.update("UPDATE comments SET status = 'DELETED' WHERE id = ?", commentId);
        if (Constants.POST_PUBLISHED.equals(comment.getStatus())) {
            jdbc.update("UPDATE posts SET comment_count = comment_count - 1 WHERE id = ? AND comment_count > 0",
                    comment.getPostId());
        }
        return Result.success();
    }

    private Comment findCommentById(Long id) {
        var list = jdbc.query("SELECT * FROM comments WHERE id = ?", RowMappers.COMMENT, id);
        return list.isEmpty() ? null : list.getFirst();
    }

    private boolean hasLiked(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE user_id = ? AND target_type = ? AND target_id = ?",
                Integer.class, userId, targetType, targetId);
        return count != null && count > 0;
    }
}
