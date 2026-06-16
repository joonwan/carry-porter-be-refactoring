package com.e101.carry_porter.global.config.mqtt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
@ConditionalOnProperty(prefix = "carry-porter.mqtt", name = "enabled", havingValue = "true")
public class RobotMqttInboundChannelConfig {

    @Bean
    public MessageChannel robotEventRouterChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel robotArrivedChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel robotReturnedChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel robotEmergencyChannel() {
        return new DirectChannel();
    }
}
