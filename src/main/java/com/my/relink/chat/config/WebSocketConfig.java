package com.my.relink.chat.config;

import com.my.relink.chat.config.metric.TaskMetric;
import com.my.relink.chat.config.metric.WebSocketMetricsHolder;
import com.my.relink.chat.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebSocketMessageBroker //STOMP 활성화
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;
    private final WebSocketMetricsHolder metricsHolder;

    /**
     * 메시지 브로커 설정
     * @param registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic") //구독 요청을 처리할 prefix
                .setHeartbeatValue(new long[]{10000, 10000}) //10초 간격으로 양방향 하트비트
                .setTaskScheduler(heartBeatScheduler()); //해당 하트비트 확인 스케줄러
        registry.setApplicationDestinationPrefixes("/app"); //클라이언트가 메시지를 발행할 때 사용할 prefix
        registry.setPreservePublishOrder(true); //메시지 순서 보장
    }

    /**백업 통신 방법 설정
     *
     * 웹소켓 연결에 실패했을 시 SockJS가 대체 전송 방식을 사용하며, 이 떄 해당 설정이 적용된다
     *
     * 웹소켓 엔드포인트 등록 및 모든 오리진에서의 접근 허용
     *
     * ex) ws:localhost:9090/chats
     * @param registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chats")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(512 * 1024) //512kb
                .setHttpMessageCacheSize(1000) //웹소켓 연결이 끊겼을 시 재연결을 위해 메시지를 캐시하는 개수. 1000개 정도가 일시적 네트워크 문제 시 대부분의 메시지를 복구할 수 있다 함
                .setDisconnectDelay(30 * 1000) //30초. 클라이언트와의 연결이 끊어졌다고 판단하기까지의 대기 시간
                .setHeartbeatTime(25000) //25초마다 연결 상태 확인
                .setWebSocketEnabled(true) //웹소켓 프로토콜 명시적 활성화
                .setSessionCookieNeeded(false); //불필요한 세션 쿠키 비활성화
    }

    /**
     * 주 통신 방법 설정
     *
     * 웹소켓 연결 성공시 해당 설정이 적용된다
     *
     * @param registry
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry
                .setMessageSizeLimit(512 * 1024)  // 메시지 크기 제한: 512kb
                .setSendBufferSizeLimit(2048 * 1024)  // 버퍼 크기 제한: 1mb
                .setSendTimeLimit(20 * 1000)  // 메시지 전송 제한 시간: 20초
                .setTimeToFirstMessage(10 * 1000)  // 첫 메시지 수신 대기 시간: 10초
                .addDecoratorFactory(webSocketHandlerDecoratorFactory()); //웹소켓 핸들러 데코레이터 추가 -> 웹소켓 생명주기 주요 이벤트 추적
    }


    @Bean
    public ThreadPoolTaskExecutor webSocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("websocket-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(webSocketTaskDecorator()); //task 추적
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskDecorator webSocketTaskDecorator() {
        return task -> {
            long queuedTime = System.nanoTime();
            return () -> {
                long startTime = System.nanoTime();
                try {
                    task.run();
                } finally {
                    long completionTime = System.nanoTime();
                    TaskMetric metric = new TaskMetric(
                            queuedTime,
                            startTime,
                            completionTime,
                            TimeUnit.NANOSECONDS.toMillis(startTime - queuedTime),
                            TimeUnit.NANOSECONDS.toMillis(completionTime - startTime)
                    );
                    metricsHolder.addMetric(metric);
                }
            };
        };
    }

    /**
     * 클라이언트로부터 들어오는 메시지를 처리하는 채널에 인터셉터 등록
     * @param registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
        registration.taskExecutor(webSocketTaskExecutor());
    }

    @Bean
    public WebSocketHandlerDecoratorFactory webSocketHandlerDecoratorFactory() {
        return handler -> new WebSocketHandlerDecorator(handler){
            public void afterConnectionEstablished(WebSocketSession session) throws Exception{
                log.debug("웹소켓 연결 성공");
                super.afterConnectionEstablished(session);
            }

            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                if (exception instanceof IOException && exception.getCause() instanceof ClosedChannelException) {
                    log.warn("WebSocket 채널이 이미 닫혔습니다 - sessionId: {}", session.getId());
                    return;
                }
                log.error("WebSocket 전송 오류 - sessionId: {}, error: {}",
                        session.getId(), exception.getMessage());
                super.handleTransportError(session, exception);
            }

            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                log.info("WebSocket 연결 종료 - sessionId: {}, status: {}",
                        session.getId(), status);
                super.afterConnectionClosed(session, status);
            }
        };
    }

    @Bean
    public TaskScheduler heartBeatScheduler(){
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); //단일스레드로 하트비트 관리
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        return scheduler;
    }
}
