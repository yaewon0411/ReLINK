package com.my.relink.chat.config;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {
    private final Cache<String, Map<String, Object>> sessionCache;
    private final Cache<Long, String> userSessionCache;

    public void addSession(String sessionId, Long userId, Map<String, Object> sessionAttribute){
        sessionCache.put(sessionId, sessionAttribute);
        userSessionCache.put(userId, sessionId);
    }

    public void addLastMessageId(Long messageId, Long userId){
        String sessionId = userSessionCache.getIfPresent(userId);
        if (sessionId == null) {
            return;
        }
        Map<String, Object> sessionAttribute = sessionCache.getIfPresent(sessionId);
        if (sessionAttribute == null) {
            return;
        }
        sessionAttribute.put("lastMessageId", messageId);
        sessionCache.put(sessionId, sessionAttribute);
    }


    /**
     * WebSocket 연결 끊김 시 세션 복구를 위해 사용
     * @param userId
     * @return
     */
    public Map<String, Object> recoverUserSession(Long userId) {
        String sessionId = userSessionCache.getIfPresent(userId);
        if(sessionId != null) {
            return sessionCache.getIfPresent(sessionId);
        }
        return null;
    }

    /**
     * WebSocket 세션 정보 제거
     *
     * @param sessionId
     * @param userId
     */
    public void removeSession(String sessionId, Long userId) {
        sessionCache.invalidate(sessionId);
        userSessionCache.invalidate(userId);
    }


}
