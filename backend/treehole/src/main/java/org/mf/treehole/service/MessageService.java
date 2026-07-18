package org.mf.treehole.service;

import jakarta.annotation.Resource;
import org.mf.treehole.common.*;
import org.mf.treehole.dto.SendMessageRequest;
import org.mf.treehole.dto.SystemMessageRequest;
import org.mf.treehole.entity.Message;
import org.mf.treehole.entity.SystemMessage;
import org.mf.treehole.mapper.RowMappers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    @Resource
    private JdbcTemplate jdbc;

    @Resource
    private UserService userService;

    public Result<Void> sendMessage(SendMessageRequest req) {
        Long senderId = UserContext.getUserId();
        Long receiverId = req.getReceiverId();

        var sender = userService.findById(senderId);
        var receiver = userService.findById(receiverId);

        if (sender == null || receiver == null) {
            return Result.error(404, "用户不存在");
        }

        // 管理员和审核员可以给任何人发消息
        boolean isStaff = Constants.ROLE_ADMIN.equals(sender.getRole()) || Constants.ROLE_REVIEWER.equals(sender.getRole());

        if (!isStaff) {
            // 检查拉黑关系
            if (userService.isEitherBlocked(senderId, receiverId)) {
                return Result.error(403, "对方已拉黑你或你已拉黑对方，无法发送消息");
            }

            // 检查关注关系
            if (!userService.isFollowing(senderId, receiverId)) {
                return Result.error(403, "请先关注对方再发送消息");
            }

            // 在收到回复之前只能发送1条消息
            if (!hasReceivedReply(senderId, receiverId)) {
                Integer unreadSent = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM messages WHERE sender_id = ? AND receiver_id = ? AND is_read = 0",
                        Integer.class, senderId, receiverId);
                if (unreadSent != null && unreadSent >= 1) {
                    return Result.error(403, "在对方回复之前，你只能发送1条消息");
                }
            }
        }

        jdbc.update("INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)",
                senderId, receiverId, req.getContent());
        return Result.success();
    }

    public Result<List<Map<String, Object>>> getConversations() {
        Long userId = UserContext.getUserId();

        List<Map<String, Object>> conversations = jdbc.query("""
                SELECT
                    IF(m.sender_id = ?, m.receiver_id, m.sender_id) AS other_user_id,
                    u.nickname AS other_nickname,
                    u.avatar AS other_avatar,
                    u.role AS other_role,
                    MAX(m.created_at) AS last_message_time,
                    (SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND sender_id = IF(m.sender_id = ?, m.receiver_id, m.sender_id) AND is_read = 0) AS unread_count
                FROM messages m
                INNER JOIN users u ON u.id = IF(m.sender_id = ?, m.receiver_id, m.sender_id)
                WHERE m.sender_id = ? OR m.receiver_id = ?
                GROUP BY other_user_id, u.nickname, u.avatar, u.role
                ORDER BY last_message_time DESC
                """, (rs, _) -> {
                    Map<String, Object> conv = new java.util.HashMap<>();
                    conv.put("userId", rs.getLong("other_user_id"));
                    conv.put("nickname", rs.getString("other_nickname"));
                    conv.put("avatar", rs.getString("other_avatar"));
                    conv.put("role", rs.getString("other_role"));
                    conv.put("lastMessageTime", rs.getTimestamp("last_message_time"));
                    conv.put("unreadCount", rs.getInt("unread_count"));
                    return conv;
                }, userId, userId, userId, userId, userId, userId);

        return Result.success(conversations);
    }

    public Result<List<Message>> getChatHistory(Long otherUserId) {
        Long currentUserId = UserContext.getUserId();

        // 标记收到的消息为已读
        jdbc.update("UPDATE messages SET is_read = 1 WHERE sender_id = ? AND receiver_id = ?",
                otherUserId, currentUserId);

        List<Message> messages = jdbc.query("""
                SELECT m.*, su.nickname AS sender_nickname, su.avatar AS sender_avatar,
                       ru.nickname AS receiver_nickname, ru.avatar AS receiver_avatar
                FROM messages m
                INNER JOIN users su ON m.sender_id = su.id
                INNER JOIN users ru ON m.receiver_id = ru.id
                WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)
                ORDER BY m.created_at
                """, (rs, rowNum) -> {
            Message m = RowMappers.MESSAGE.mapRow(rs, rowNum);
            m.setSenderNickname(rs.getString("sender_nickname"));
            m.setSenderAvatar(rs.getString("sender_avatar"));
            m.setReceiverNickname(rs.getString("receiver_nickname"));
            m.setReceiverAvatar(rs.getString("receiver_avatar"));
            return m;
        }, currentUserId, otherUserId, otherUserId, currentUserId);

        return Result.success(messages);
    }

    public Result<Integer> getUnreadCount() {
        Long userId = UserContext.getUserId();
        Integer msgCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = 0",
                Integer.class, userId);
        Integer sysCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_messages WHERE (user_id = ? OR user_id IS NULL) AND is_read = 0",
                Integer.class, userId);
        int total = (msgCount != null ? msgCount : 0) + (sysCount != null ? sysCount : 0);
        return Result.success(total);
    }

    // 系统消息
    public Result<Void> sendSystemMessage(SystemMessageRequest req) {
        if (!UserContext.isAdmin()) {
            return Result.error(403, "无权操作");
        }
        jdbc.update("INSERT INTO system_messages (user_id, title, content) VALUES (?, ?, ?)",
                req.getUserId(), req.getTitle(), req.getContent());
        return Result.success();
    }

    public Result<List<SystemMessage>> getSystemMessages() {
        Long userId = UserContext.getUserId();
        List<SystemMessage> messages = jdbc.query("""
                SELECT * FROM system_messages
                WHERE user_id = ? OR user_id IS NULL
                ORDER BY created_at DESC
                LIMIT 50
                """, RowMappers.SYSTEM_MESSAGE, userId);
        return Result.success(messages);
    }

    public Result<Void> readSystemMessage(Long messageId) {
        Long userId = UserContext.getUserId();
        jdbc.update("UPDATE system_messages SET is_read = 1 WHERE id = ? AND (user_id = ? OR user_id IS NULL)",
                messageId, userId);
        return Result.success();
    }

    public void sendSystemMessageToUser(Long userId, String title, String content) {
        jdbc.update("INSERT INTO system_messages (user_id, title, content) VALUES (?, ?, ?)",
                userId, title, content);
    }

    private boolean hasReceivedReply(Long senderId, Long receiverId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE sender_id = ? AND receiver_id = ?",
                Integer.class, receiverId, senderId);
        return count != null && count > 0;
    }
}
