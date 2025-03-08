package com.my.relink.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.my.relink.chat.config.cache.SendFailedMessagesCacheKey;
import com.my.relink.chat.controller.dto.response.ChatMessageRespDto;
import com.my.relink.chat.event.MessageSaveFailedEvent;
import com.my.relink.chat.event.MessageSendRetryEvent;
import com.my.relink.domain.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRetryService {

    private final Cache<Long, List<Map<String, Object>>> sendFailedMessagesCache;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;


    @Async
    @EventListener
    public void handleMessageRetryEvent(MessageSendRetryEvent event) {
        log.debug("메시지 재전송 이벤트 수신- userId: {}", event.getUserId());
        resendSendFailedMessages(event.getUserId());
    }


    public void resendSendFailedMessages(Long userId) {
        List<Map<String, Object>> failedMessages = sendFailedMessagesCache.getIfPresent(userId);
        if(failedMessages != null && !failedMessages.isEmpty()) {
            for (Map<String, Object> failedMessage : failedMessages) {
                String topicPath = failedMessage.get(SendFailedMessagesCacheKey.TOPIC_PATH.getValue()).toString();
                ChatMessageRespDto message = (ChatMessageRespDto) failedMessage.get(SendFailedMessagesCacheKey.MESSAGE.getValue());
                try {
                    messagingTemplate.convertAndSend(topicPath, message);
                    log.info("재전송 성공- userId: {}, destination: {}", userId, topicPath);
                } catch (Exception ex) {
                    log.error("재전송 실패- userId: {}, destination: {}, error: {}", userId, topicPath, ex.getMessage());
                }
            }
        }
        sendFailedMessagesCache.invalidate(userId);
    }

    @Async
    @Transactional
    public void saveMessageAsync(Message message){
        try {
            log.info(Thread.currentThread().getName()+" 별도 스레드가 맞니???");
            //messageRepository.save(message);
        }catch (Exception e){
            log.error("비동기 메시지 저장 실패: senderId: {}, tradeId: {}, messageTime: {}", message.getUser().getId(), message.getTrade().getId(), message.getMessageTime());
            eventPublisher.publishEvent(new MessageSaveFailedEvent(message));
        }
    }

}
