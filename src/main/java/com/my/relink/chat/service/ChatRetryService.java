package com.my.relink.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.my.relink.chat.controller.dto.response.ChatMessageRespDto;
import com.my.relink.domain.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class ChatRetryService {

    private final Cache<Long, List<Map<String, Object>>> sendFailedMessagesCache;
    private final SimpMessageSendingOperations messagingTemplate;
    private final String TOPIC_PATH = "topic_path";
    private final String MESSAGE = "message";


    public void saveSendFailedMessage(String topic, ChatMessageRespDto response, Long tradeId) {
        Long userId = response.getSenderId();
        List<Map<String, Object>> failedMessages = sendFailedMessagesCache.getIfPresent(userId);
        if(failedMessages == null){
            failedMessages = new CopyOnWriteArrayList<>();
        }
        Map<String, Object> data = new HashMap<>();
        data.put(TOPIC_PATH, topic+tradeId);
        data.put(MESSAGE, response);
        failedMessages.add(data);
    }


    public void resendSendFailedMessages(Long userId) {
        List<Map<String, Object>> failedMessages = sendFailedMessagesCache.getIfPresent(userId);
        if(failedMessages != null && !failedMessages.isEmpty()) {
            for (Map<String, Object> failedMessage : failedMessages) {
                messagingTemplate.convertAndSend(failedMessage.get(TOPIC_PATH).toString(), failedMessage.get(MESSAGE));
            }
        }
        sendFailedMessagesCache.invalidate(userId);
    }

}
