package org.mf.treehole.service;

import jakarta.annotation.Resource;
import org.mf.treehole.common.*;
import org.mf.treehole.dto.HandleReportRequest;
import org.mf.treehole.dto.ReportRequest;
import org.mf.treehole.entity.Report;
import org.mf.treehole.entity.User;
import org.mf.treehole.mapper.RowMappers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Resource
    private JdbcTemplate jdbc;

    @Resource
    private UserService userService;

    @Resource
    private MessageService messageService;

    private static final Map<String, String> REPORT_TYPE_LABELS = Map.of(
            "PORN", "色情低俗",
            "POLITICAL", "涉政敏感",
            "CYBERBULLY", "引战网暴",
            "FRAUD", "涉嫌诈骗",
            "RUMOR", "传播谣言",
            "OTHER", "其它"
    );

    public Result<Void> createReport(ReportRequest req) {
        Long reporterId = UserContext.getUserId();

        // 验证举报类型
        if (!REPORT_TYPE_LABELS.containsKey(req.getReportType())) {
            return Result.error(400, "无效的举报类型");
        }

        // OTHER类型必须填写理由
        if ("OTHER".equals(req.getReportType()) && (req.getReason() == null || req.getReason().isBlank())) {
            return Result.error(400, "举报类型为'其它'时必须填写举报理由");
        }

        // 验证目标存在
        if (Constants.TARGET_POST.equals(req.getTargetType())) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE id = ?", Integer.class, req.getTargetId());
            if (count == null || count == 0) {
                return Result.error(404, "举报的帖子不存在");
            }
        } else if (Constants.TARGET_COMMENT.equals(req.getTargetType())) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE id = ?", Integer.class, req.getTargetId());
            if (count == null || count == 0) {
                return Result.error(404, "举报的评论不存在");
            }
        } else {
            return Result.error(400, "无效的举报目标类型");
        }

        // 不能举报自己的内容
        Long targetOwnerId = getTargetOwnerId(req.getTargetType(), req.getTargetId());
        if (targetOwnerId != null && targetOwnerId.equals(reporterId)) {
            return Result.error(400, "不能举报自己的内容");
        }

        // 不能举报管理员的内容
        if (targetOwnerId != null) {
            User targetOwner = userService.findById(targetOwnerId);
            if (targetOwner != null && Constants.ROLE_ADMIN.equals(targetOwner.getRole())) {
                return Result.error(403, "不能举报管理员的内容");
            }
        }

        // 检查是否已举报过
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE reporter_id = ? AND target_type = ? AND target_id = ?",
                Integer.class, reporterId, req.getTargetType(), req.getTargetId());
        if (existing != null && existing > 0) {
            return Result.error(400, "您已举报过此内容");
        }

        jdbc.update("""
                INSERT INTO reports (reporter_id, target_type, target_id, report_type, reason, status)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """, reporterId, req.getTargetType(), req.getTargetId(), req.getReportType(), req.getReason());

        return Result.success("举报已提交，管理员将尽快处理", null);
    }

    public Result<List<Report>> getPendingReports(int page, int size) {
        if (!UserContext.isReviewer()) {
            return Result.error(403, "无权操作");
        }
        int offset = (page - 1) * size;
        List<Report> reports = jdbc.query("""
                SELECT r.*, u.nickname AS reporter_nickname, h.nickname AS handler_nickname
                FROM reports r
                LEFT JOIN users u ON r.reporter_id = u.id
                LEFT JOIN users h ON r.handler_id = h.id
                WHERE r.status = 'PENDING'
                ORDER BY r.created_at ASC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            Report r = RowMappers.REPORT.mapRow(rs, rowNum);
            r.setReporterNickname(rs.getString("reporter_nickname"));
            r.setHandlerNickname(rs.getString("handler_nickname"));
            // 获取被举报内容
            r.setTargetContent(getTargetContent(r.getTargetType(), r.getTargetId()));
            return r;
        }, size, offset);

        return Result.success(reports);
    }

    public Result<Void> handleReport(HandleReportRequest req) {
        if (!UserContext.isReviewer()) {
            return Result.error(403, "无权操作");
        }

        Long handlerId = UserContext.getUserId();
        Report report = findReportById(req.getReportId());
        if (report == null) {
            return Result.error(404, "举报记录不存在");
        }
        if (!Constants.REPORT_PENDING.equals(report.getStatus())) {
            return Result.error(400, "该举报已处理");
        }

        String status = req.getStatus();
        if (!Constants.REPORT_APPROVED.equals(status) && !Constants.REPORT_REJECTED.equals(status)) {
            return Result.error(400, "无效的处理结果");
        }

        Integer penalty = 0;
        if (Constants.REPORT_APPROVED.equals(status)) {
            penalty = req.getPenalty() != null ? req.getPenalty() : 5;
            // 扣除被举报内容作者的信誉分
            Long targetOwnerId = getTargetOwnerId(report.getTargetType(), report.getTargetId());
            if (targetOwnerId != null) {
                User owner = userService.findById(targetOwnerId);
                if (owner != null) {
                    int newScore = Math.max(0, owner.getCreditScore() - penalty);
                    String newStatus = newScore < Constants.CREDIT_NEED_REVIEW ? Constants.STATUS_BANNED : owner.getStatus();
                    jdbc.update("UPDATE users SET credit_score = ?, status = ? WHERE id = ?",
                            newScore, newStatus, targetOwnerId);

                    // 删除违规内容
                    if (Constants.TARGET_POST.equals(report.getTargetType())) {
                        jdbc.update("UPDATE posts SET status = 'DELETED' WHERE id = ?", report.getTargetId());
                    } else {
                        jdbc.update("UPDATE comments SET status = 'DELETED' WHERE id = ?", report.getTargetId());
                    }

                    // 通知被举报用户
                    messageService.sendSystemMessageToUser(targetOwnerId, "举报处理通知",
                            "您的内容因\"" + REPORT_TYPE_LABELS.getOrDefault(report.getReportType(), "违规") + "\"被举报成立，扣除信誉分" + penalty + "分。");
                }
            }
        }

        jdbc.update("""
                UPDATE reports SET status = ?, handler_id = ?, penalty = ?, handled_at = NOW()
                WHERE id = ?
                """, status, handlerId, penalty, req.getReportId());

        // 通知举报人
        messageService.sendSystemMessageToUser(report.getReporterId(), "举报受理通知",
                Constants.REPORT_APPROVED.equals(status)
                        ? "您的举报已受理，处理结果：举报成立，已对违规内容进行处理。"
                        : "您的举报已受理，处理结果：举报不成立。");

        return Result.success();
    }

    private Report findReportById(Long id) {
        var list = jdbc.query("SELECT * FROM reports WHERE id = ?", RowMappers.REPORT, id);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long getTargetOwnerId(String targetType, Long targetId) {
        String table = Constants.TARGET_POST.equals(targetType) ? "posts" : "comments";
        var list = jdbc.query("SELECT user_id FROM " + table + " WHERE id = ?",
                (rs, rowNum) -> rs.getLong("user_id"), targetId);
        return list.isEmpty() ? null : list.get(0);
    }

    private String getTargetContent(String targetType, Long targetId) {
        String table = Constants.TARGET_POST.equals(targetType) ? "posts" : "comments";
        var list = jdbc.query("SELECT content FROM " + table + " WHERE id = ?",
                (rs, rowNum) -> rs.getString("content"), targetId);
        if (list.isEmpty()) return "[内容已删除]";
        String content = list.get(0);
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    public static String getReportTypeLabel(String type) {
        return REPORT_TYPE_LABELS.getOrDefault(type, type);
    }
}
