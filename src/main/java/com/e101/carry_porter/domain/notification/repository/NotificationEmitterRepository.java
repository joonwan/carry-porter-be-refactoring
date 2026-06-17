package com.e101.carry_porter.domain.notification.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class NotificationEmitterRepository {

	// userId 기준으로 현재 활성화된 SSE emitter 를 메모리에 저장
	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

	public Optional<SseEmitter> findByUserId(Long userId) {
		return Optional.ofNullable(emitters.get(userId));
	}

	public void save(Long userId, SseEmitter emitter) {
		emitters.put(userId, emitter);
	}

	public void delete(Long userId, SseEmitter emitter) {
		emitters.computeIfPresent(userId, (key, currentEmitter) ->
				currentEmitter == emitter ? null : currentEmitter
		);
	}
}
