package com.marcura.exchangerate.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/** JDBC-backed ShedLock so only one pod runs {@code fixer-daily-ingest} at a time. */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
public class SchedulingConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withTableName("shedlock")
                        .usingDbTime()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .build());
    }
}
