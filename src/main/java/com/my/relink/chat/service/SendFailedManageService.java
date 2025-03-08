package com.my.relink.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.my.relink.chat.config.cache.SendFailedMessagesCacheKey;
import com.my.relink.chat.controller.dto.response.ChatMessageRespDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class SendFailedManageService {

    private final Cache<Long, List<Map<String, Object>>> sendFailedMessagesCache;

    public void saveSendFailedMessage(String topic, ChatMessageRespDto response, Long tradeId) {
        Long userId = response.getSenderId();
        List<Map<String, Object>> failedMessages = sendFailedMessagesCache.getIfPresent(userId);
        if(failedMessages == null){
            failedMessages = new CopyOnWriteArrayList<>();
            sendFailedMessagesCache.put(userId, failedMessages);
        }
        Map<String, Object> data = new HashMap<>();
        data.put(SendFailedMessagesCacheKey.TOPIC_PATH.getValue(), topic+tradeId);
        data.put(SendFailedMessagesCacheKey.MESSAGE.getValue(), response);
        failedMessages.add(data);
    }

    public void saveSendFailedMessage(String topicPath, ChatMessageRespDto response){
        Long userId = response.getSenderId();
        List<Map<String, Object>> failedMessages = sendFailedMessagesCache.getIfPresent(userId);
        if(failedMessages == null){
            failedMessages = new CopyOnWriteArrayList<>();
            sendFailedMessagesCache.put(userId, failedMessages);
        }
        Map<String, Object> data = new HashMap<>();
        data.put(SendFailedMessagesCacheKey.TOPIC_PATH.getValue(), topicPath);
        data.put(SendFailedMessagesCacheKey.MESSAGE.getValue(), response);
        failedMessages.add(data);
    }
}
