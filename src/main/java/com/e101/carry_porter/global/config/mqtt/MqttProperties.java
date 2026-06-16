package com.e101.carry_porter.global.config.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "carry-porter.mqtt")
public class MqttProperties {

    private boolean enabled;
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private long completionTimeout;
    private final Inbound inbound = new Inbound();
    private final Outbound outbound = new Outbound();

    @Getter
    @Setter
    public static class Inbound {
        private String topic;
        private int qos;
    }

    @Getter
    @Setter
    public static class Outbound {
        private String defaultTopic;
        private int qos;
    }
}
