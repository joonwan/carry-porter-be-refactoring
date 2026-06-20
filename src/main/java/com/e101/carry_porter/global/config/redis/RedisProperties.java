package com.e101.carry_porter.global.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "carry-porter.redis")
public class RedisProperties {

    private final PubSub pubSub = new PubSub();

    @Getter
    @Setter
    public static class PubSub {
        private String notificationChannel;
    }
}
