package org.mf.treehole.config;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Resource
    private JdbcTemplate jdbc;

    /**
     * 每天凌晨0点重置每日登录加分标记
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyLogin() {
        jdbc.update("UPDATE users SET daily_login_checked = 0");
    }
}
