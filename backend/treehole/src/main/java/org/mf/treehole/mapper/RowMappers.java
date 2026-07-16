package org.mf.treehole.mapper;

import org.mf.treehole.entity.*;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class RowMappers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final RowMapper<User> USER = (rs, _) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setNickname(rs.getString("nickname"));
        u.setAvatar(rs.getString("avatar"));
        u.setBio(rs.getString("bio"));
        u.setRole(rs.getString("role"));
        u.setCreditScore(rs.getInt("credit_score"));
        u.setStatus(rs.getString("status"));
        u.setLikeReceivedCount(rs.getInt("like_received_count"));
        u.setDailyLoginChecked(rs.getInt("daily_login_checked"));
        u.setCreatedAt(getDateTime(rs, "created_at"));
        u.setUpdatedAt(getDateTime(rs, "updated_at"));
        return u;
    };

    public static final RowMapper<Post> POST = (rs, _) -> {
        Post p = new Post();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("user_id"));
        p.setBoard(rs.getString("board"));
        p.setContent(rs.getString("content"));
        String imagesJson = rs.getString("images");
        if (imagesJson != null && !imagesJson.isEmpty() && !imagesJson.equals("null")) {
            try {
                p.setImages(MAPPER.readValue(imagesJson, new TypeReference<>() {}));
            } catch (Exception e) {
                p.setImages(List.of());
            }
        }
        p.setLikeCount(rs.getInt("like_count"));
        p.setCommentCount(rs.getInt("comment_count"));
        p.setShareCount(rs.getInt("share_count"));
        p.setStatus(rs.getString("status"));
        p.setRobotResponse(rs.getString("robot_response"));
        p.setRobotResponded(rs.getBoolean("robot_responded"));
        p.setCreatedAt(getDateTime(rs, "created_at"));
        p.setUpdatedAt(getDateTime(rs, "updated_at"));
        return p;
    };

    public static final RowMapper<Comment> COMMENT = (rs, _) -> {
        Comment c = new Comment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setUserId(rs.getLong("user_id"));
        c.setContent(rs.getString("content"));
        long parentId = rs.getLong("parent_id");
        c.setParentId(rs.wasNull() ? null : parentId);
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(getDateTime(rs, "created_at"));
        return c;
    };

    public static final RowMapper<Message> MESSAGE = (rs, _) -> {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setReceiverId(rs.getLong("receiver_id"));
        m.setContent(rs.getString("content"));
        m.setIsRead(rs.getBoolean("is_read"));
        m.setCreatedAt(getDateTime(rs, "created_at"));
        return m;
    };

    public static final RowMapper<SystemMessage> SYSTEM_MESSAGE = (rs, _) -> {
        SystemMessage sm = new SystemMessage();
        sm.setId(rs.getLong("id"));
        long userId = rs.getLong("user_id");
        sm.setUserId(rs.wasNull() ? null : userId);
        sm.setTitle(rs.getString("title"));
        sm.setContent(rs.getString("content"));
        sm.setIsRead(rs.getBoolean("is_read"));
        sm.setCreatedAt(getDateTime(rs, "created_at"));
        return sm;
    };

    public static final RowMapper<Report> REPORT = (rs, _) -> {
        Report r = new Report();
        r.setId(rs.getLong("id"));
        r.setReporterId(rs.getLong("reporter_id"));
        r.setTargetType(rs.getString("target_type"));
        r.setTargetId(rs.getLong("target_id"));
        r.setReportType(rs.getString("report_type"));
        r.setReason(rs.getString("reason"));
        r.setStatus(rs.getString("status"));
        long handlerId = rs.getLong("handler_id");
        r.setHandlerId(rs.wasNull() ? null : handlerId);
        int penalty = rs.getInt("penalty");
        r.setPenalty(rs.wasNull() ? null : penalty);
        r.setHandledAt(getDateTime(rs, "handled_at"));
        r.setCreatedAt(getDateTime(rs, "created_at"));
        return r;
    };

    public static String toJson(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime getDateTime(ResultSet rs, String column) {
        try {
            Timestamp ts = rs.getTimestamp(column);
            return ts != null ? ts.toLocalDateTime() : null;
        } catch (SQLException e) {
            return null;
        }
    }
}
