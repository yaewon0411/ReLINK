package com.my.relink.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.my.relink.chat.service.dto.MessageTask;
import com.my.relink.controller.payment.dto.request.PaymentReqDto;
import com.my.relink.domain.message.Message;
import com.my.relink.domain.message.repository.MessageRepository;
import com.my.relink.domain.user.User;
import io.sentry.Scope;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageQueueService {

    private final Cache<String, Queue<MessageTask>> saveFailedMessageCache;
    private final Cache<String, MessageTask> processingSaveMessage;
    private final Cache<String, MessageTask> retryFailedMessageQueue;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private final MessageRepository messageRepository;
    private final static String QUEUE_KEY = "message-queue";
    private final static int MAX_TRY = 3;

    @PostConstruct
    public void start(){
        processMessageSave();
    }

    @PreDestroy
    public void shutdown(){
        scheduledExecutorService.shutdown();
    }

    public void saveSaveFailedMessage(Message message) {
        Queue<MessageTask> queue = saveFailedMessageCache.get(QUEUE_KEY, key -> new ConcurrentLinkedQueue<>());
        queue.add(new MessageTask(message, 0));
    }

    private void processMessageSave() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try{
                processBatchMessageSave();
            } catch (Exception e){
                log.error("메시지 저장 중 오류 발생: " + e.getMessage(), e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void processBatchMessageSave() {
        Queue<MessageTask> queue = saveFailedMessageCache.getIfPresent(QUEUE_KEY);
        if(queue == null || queue.isEmpty()){
            return;
        }

        for(int i = 0; i<10; i++){
            MessageTask messageTask = queue.poll();
            if(messageTask == null){
                break;
            }
            String userId = messageTask.getMessage().getUser().getId().toString();
            String messageTime = messageTask.getMessage().getMessageTime().toString();
            StringBuilder key = new StringBuilder(userId).append("+").append(messageTime);
            processingSaveMessage.put(key.toString(), messageTask);

            try{
                messageRepository.save(messageTask.getMessage());
                processingSaveMessage.invalidate(key.toString());
            }catch (Exception e){
                log.error("[MessageQueueService] 메시지 저장 실패: " + key, e);
                processingSaveMessage.invalidate(key.toString());

                if (messageTask.getCurrentTry() < MAX_TRY) {
                    messageTask.setCurrentTry(messageTask.getCurrentTry() + 1);
                    long delayMs = 200 * (long) Math.pow(2, messageTask.getCurrentTry());
                    scheduledExecutorService.schedule(() -> {
                        queue.add(messageTask);
                    }, delayMs, TimeUnit.MILLISECONDS);

                } else {
                    retryFailedMessageQueue.put(key.toString(), messageTask);
                    sendRetryFailureAlert(messageTask, e);
                }
            }
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
