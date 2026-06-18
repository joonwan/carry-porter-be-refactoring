package com.e101.carry_porter.global.config.scheduling;

import com.e101.carry_porter.domain.notification.config.NotificationCleanupProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationCleanupProperties.class)
public class SchedulingConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
