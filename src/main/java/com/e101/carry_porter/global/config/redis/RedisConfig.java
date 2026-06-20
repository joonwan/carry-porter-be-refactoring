package com.e101.carry_porter.global.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;

    /**
     *  spring 에서 Redis 에 데이터를 넣고 꺼낼때. key, value 를 어떤 형식으로 변환할지 정하는 설정
     *  redisTemplate 은 Java code 에서 Redis 명령을 실행하게 해주는 도구
     *  key: String, value: 여러 객체 가능
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // redis 객체 생성
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // redis value 를 JSON 으로 바꿔주는 직렬화기 생성
        // redis 는 기본적으로 byte 배열 저장. 따라서 중간에 변환 필요
        // GenericJackson2JsonRedisSerializer 는 Jackson 을 이용해서 객체를 JSON 으로 변경함
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // RedisTemplate 이 사용할 Redis 연결 공장 지정
        // RedisConnectionFactory 는 Redis 서버와의 연결을 만들어주는 객체
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // redis 의 일반 key 를 문자열로 저장
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // redis hash 안의 field key 를 문자열로 저장
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // redis 의 일반 value 를 json 으로 저장
        redisTemplate.setValueSerializer(serializer);
        // redis hash 안의 value 도 json 으로 저장
        redisTemplate.setHashValueSerializer(serializer);

        // redis template 을 설정에 맞추어 초기화
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     *
     * RedisMessageListenerContainer: redis pub/sub 구독 담당하는 container
     * Redis channel 을 계속 듣고 있다가 메시지가 오면 Listener 에게 넘겨주는 객체
     * redisConnectionFactory 는 SpringBoot 가 application.yml 보고 자동으로 만들어줌
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // container 가 redis 접속시 사용할 연결 정보를 설정
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

    /**
     * channel topic 을 spring bean 으로 등록
     * @return
     */
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic(redisProperties.getPubSub().getNotificationChannel());
    }
}
