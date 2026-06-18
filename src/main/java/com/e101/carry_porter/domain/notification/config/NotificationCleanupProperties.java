package com.e101.carry_porter.domain.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "carry-porter.notification.cleanup")
public class NotificationCleanupProperties {

    private int retentionDays;
    private String cron;
}
