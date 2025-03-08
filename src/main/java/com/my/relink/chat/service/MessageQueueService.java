package com.my.relink.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.my.relink.chat.event.MessageSaveFailedEvent;
import com.my.relink.chat.service.dto.MessageTask;
import com.my.relink.domain.message.Message;
import com.my.relink.domain.message.repository.MessageRepository;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageQueueService {

    private final Cache<String, Queue<MessageTask>> saveFailedMessageCache;
    @Qualifier("retryFailedMessage")
    private final Cache<String, MessageTask> retryFailedMessage;
    private final ExecutorService es = Executors.newFixedThreadPool(5);
    private final MessageRepository messageRepository;
    private final static String QUEUE_KEY = "message-queue";
    private final static int MAX_TRY = 3;


    public void saveSaveFailedMessage(Message message) {
        Queue<MessageTask> queue = saveFailedMessageCache.get(QUEUE_KEY, key -> new ConcurrentLinkedQueue<>());
        queue.add(new MessageTask(message, 0));
    }

    @EventListener
    @Transactional
    public void handleMessageSaveFailedEvent(MessageSaveFailedEvent event){
        try{
            Message message = messageRepository.save(event.getMessage());
            log.info("[재시도로 메시지 저장 성공] :" + message.getId());
        }catch (Exception e){
            log.error("메시지 저장 재시도 실패: {}", e.getMessage());
            saveSaveFailedMessage(event.getMessage());
        }
    }


    @Scheduled(fixedDelay = 10000)
    public void processBatchMessageSave() {
        Queue<MessageTask> queue = saveFailedMessageCache.getIfPresent(QUEUE_KEY);
        if(queue == null || queue.isEmpty()){
            return;
        }

        for(int i = 0; i<10; i++) {
            es.execute(() -> {
                MessageTask messageTask = queue.poll();
                if (messageTask == null) {
                    return;
                }
                String userId = messageTask.getMessage().getUser().getId().toString();
                String messageTime = messageTask.getMessage().getMessageTime().toString();
                StringBuilder key = new StringBuilder(userId).append("+").append(messageTime);

                try {
                    messageRepository.save(messageTask.getMessage());
                } catch (Exception e) {
                    log.error("[MessageQueueService] 메시지 저장 실패: " + key, e);

                    if (messageTask.getCurrentTry() < MAX_TRY) {
                        messageTask.setCurrentTry(messageTask.getCurrentTry() + 1);
                        queue.add(messageTask);
                    } else {
                        retryFailedMessage.put(key.toString(), messageTask);
                        sendRetryFailureAlert(messageTask, e);
                    }
                }
            });
        }

    }

    @PreDestroy
    public void shutdown(){
        es.shutdown();
        try {
            if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
                es.shutdownNow();
            }
        } catch (InterruptedException e) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void sendRetryFailureAlert(MessageTask messageTask, Exception originalError) {
        Long userId = messageTask.getMessage().getUser().getId();
        Long tradeId = messageTask.getMessage().getTrade().getId();
        String title = "[메시지 저장 재시도 실패]\n";
        String detailedLog = String.format("""
            에러 전송 시각: %s
            사용자 ID: %d
            거래 ID: %d
            에러: %s
            메시지 발행 시각: %s
            """,
                LocalDateTime.now(),
                userId,
                tradeId,
                originalError.getMessage(),
                messageTask.getMessage().getMessageTime().toString()
        );

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.ERROR);
            scope.setTag("alertType", "SERVER_ERROR");
            scope.setExtra("details", title + detailedLog);
        });

        Sentry.captureException(originalError);
        log.info("[메시지 저장 재시도 실패] Sentry 알림 발송 완료. senderId: {}, tradeId: {}, e.getMessage: {}", userId, tradeId, originalError.getMessage());
    }



}
