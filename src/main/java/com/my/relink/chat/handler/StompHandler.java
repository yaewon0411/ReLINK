package com.my.relink.chat.handler;

import com.my.relink.chat.config.ChatPrincipal;
import com.my.relink.chat.config.WebSocketSessionManager;
import com.my.relink.chat.handler.metric.OperationMetrics;
import com.my.relink.chat.service.ChatRetryService;
import com.my.relink.config.security.AuthUser;
import com.my.relink.config.security.jwt.JwtProvider;
import com.my.relink.domain.trade.Trade;
import com.my.relink.domain.trade.TradeStatus;
import com.my.relink.ex.BusinessException;
import com.my.relink.ex.ErrorCode;
import com.my.relink.ex.SecurityFilterChainException;
import com.my.relink.service.TradeService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.my.relink.chat.handler.metric.OperationMetrics.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompHandler implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final TradeService tradeService;
    private final Map<String, AtomicReference<OperationMetrics>> metricsMap = new ConcurrentHashMap<>();
    private final WebSocketSessionManager sessionManager;
    private final static String SESSION_ATTRIBUTE_KEY = "USER_ID";
    private final ChatRetryService chatRetryService;



    /**
     * 소켓 연결 검증 핸들러
     *
     * - CONNECT: 초기 웹소켓 연결 시 토큰 및 거래 상태 검증
     * - SUBSCRIBE: 특정 채팅방 구독 시 해당 거래 접근 권한 검증
     *
     * @param message
     * @param channel
     * @return
     */

    //TODO jwt 관련 exception 추가되면 에러 catch 추가할 것
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        long startTime = System.currentTimeMillis();

        try {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) { //사용자 인증 및 초기화
                handleConnect(accessor);
                recordMetrics("CONNECT", System.currentTimeMillis() - startTime);
            } else if(StompCommand.CONNECTED.equals(accessor.getCommand())){ //세션 저장
                handleConnected(accessor);
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
                handleSubscribe(accessor);
                recordMetrics("SUBSCRIBE", System.currentTimeMillis() - startTime);
            } else if (StompCommand.SEND.equals(accessor.getCommand())){
                handleSend(accessor);
                recordMetrics("SEND", System.currentTimeMillis() - startTime);
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())){
                handleDisconnect(accessor);
            }
            return message;
        }catch (BusinessException e){
            log.warn("웹소켓 연결 검증 중 오류 발생: {}", e.getMessage(), e);
            return createErrorMessage(accessor, e);
        }
    }

    private void recordMetrics(String operation, long latency) {
        metricsMap.computeIfAbsent(operation,
                        k -> new AtomicReference<>(new OperationMetrics()))
                .updateAndGet(metrics -> metrics.addLatency(latency));
    }


    public Map<String, MetricsDTO> getMetrics() {
        return metricsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            OperationMetrics metrics = entry.getValue().get();
                            return new MetricsDTO(
                                    metrics.getCount(),
                                    metrics.getAverageLatency(),
                                    metrics.getMaxLatency(),
                                    metrics.get95thPercentile()
                            );
                        }
                ));
    }


    private Message<?> createErrorMessage(StompHeaderAccessor accessor, BusinessException e){
        StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        errorAccessor.setNativeHeader("status", String.valueOf(e.getErrorCode().getStatus()));
        errorAccessor.setSessionId(accessor.getSessionId());

        MessageHeaders headers = errorAccessor.getMessageHeaders();
        return MessageBuilder.createMessage(e.getMessage().getBytes(), headers);
    }


    /**
     * 채팅 시 거래 상태 검증
     * 1. 메시지를 전송하려는 채팅방의 tradeId 추출
     * 2. 거래 상태 검증
     *
     * @param accessor
     */
    private void handleSend(StompHeaderAccessor accessor){
        String destination = accessor.getDestination();

        if(destination != null && destination.startsWith("/app/chats")){
            Long tradeId = extractTradeIdFromSendPath(destination);
            TradeStatus tradeStatus = tradeService.findByIdOrFailWhenSend(tradeId);
            validateTradeStatus(tradeStatus);
        }
    }


    /**
     * 세션 정보 제거
     * @param accessor
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Long userId =  (Long) accessor.getSessionAttributes().get(SESSION_ATTRIBUTE_KEY);
        sessionManager.removeSession(sessionId, userId);
    }


    /**
     * 초기 웹소켓 연결 시 토큰 및 거래 상태 검증
     *
     * 1. jwt 검증을 통한 사용자 인증
     * 2. 거래 상태 확인
     * 3. 인증된 사용자 정보 Principal로 설정
     * @param accessor
     */
    private void handleConnect(StompHeaderAccessor accessor){
        AuthUser authUser = validateToken(accessor);
        Long userId = authUser.getId();
        String tradeStatus = accessor.getFirstNativeHeader(WebSocketHeader.TRADE_STATUS_HEADER);
        if(tradeStatus == null){
            throw new BusinessException(ErrorCode.TRADE_STATUS_NOT_FOUND);
        }
        validateTradeStatus(TradeStatus.statusOf(tradeStatus));
        accessor.setUser(new ChatPrincipal(authUser)); //웹소켓 연결 종료 시 자동으로 제거된다
        Map<String, Object> prevSessionAttribute = sessionManager.recoverUserSession(userId);
        if (prevSessionAttribute != null) {
            accessor.setSessionAttributes(prevSessionAttribute); //세션 복원
            chatRetryService.resendSendFailedMessages(userId);
        }

    }


    /**
     * 세션 정보 저장
     * @param accessor
     */
    private void handleConnected(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        AuthUser user = (AuthUser) accessor.getUser();
        sessionManager.addSession(sessionId, user.getId(), accessor.getSessionAttributes());
    }


    /**
     * 특정 채팅방 구독 시 해당 거래 접근 권한 검증
     *
     * 1. 구독하려는 채팅방의 tradeId 추출
     * 2. 해당 거래의 참여자인지 확인
     * 3. 거래 상태 검증
     * @param accessor
     */
    private void handleSubscribe(StompHeaderAccessor accessor){
        String destination = accessor.getDestination();

        if(destination != null && destination.startsWith("/topic/chats")){
            Long tradeId = extractTradeIdFromSubscribePath(destination);
            ChatPrincipal principal = (ChatPrincipal) accessor.getUser();
            Trade trade = tradeService.findByIdWithUsersOrFail(tradeId);

            trade.validateAccess(principal.getUserId());
            validateTradeStatus(trade.getTradeStatus());
            log.debug("해당 거래 채팅방 접근 검증 완료 - tradeId: {}, userId: {}", tradeId, principal.getUserId());
        }
    }


    /**
     * jwt 검증
     * TODO 추후 jwt 관련 예외 보충 예정
     * @param accessor
     * @return
     */
    private AuthUser validateToken(StompHeaderAccessor accessor){
        String tokenWithPrefix = accessor.getFirstNativeHeader(WebSocketHeader.AUTH_HEADER);
        if(tokenWithPrefix == null){
            throw new BusinessException(ErrorCode.TOKEN_NOT_FOUND);
        }
        String token = tokenWithPrefix.replace(JwtProvider.TOKEN_PREFIX, "");
        try{
            jwtProvider.validateToken(token);
        } catch (SecurityFilterChainException e){
            throw new BusinessException(e.getErrorCode());
        }
        return jwtProvider.getAuthUserForToken(token);
    }


    /**
     * 거래 상태 검증
     * EXCHANGED나 CANCELED, UNAVAILABLE 상태의 거래는 채팅 접근 불가능
     *
     * @param tradeStatus 검증할 거래 상태
     * @throws BusinessException 거래가 이미 종료된 상태(EXCHANGED/CANCELED/UNAVAILABLE)인 경우
     */
    private void validateTradeStatus(TradeStatus tradeStatus) {
        log.info("거래 상태 검증: {}", tradeStatus);
        if (!TradeStatus.isChatAccessStatus(tradeStatus)) {
            log.debug("더 이상 채팅 세션을 제공하지 않는 거래 채팅방에 접근 시도 - tradeStatus : {}", tradeStatus);
            throw new BusinessException(ErrorCode.CHATROOM_ACCESS_DENIED);
        }
    }


    /**
     * STOMP destication 경로에서 tradeId 추출
     * ex) /topic/chats/11 -> 11
     * @param destination
     * @return 추출된 tradeId
     */
    private Long extractTradeIdFromSubscribePath(String destination) {
        String[] paths = destination.split("/");
        return Long.parseLong(paths[paths.length - 1]);
    }

    private Long extractTradeIdFromSendPath(String destination){
        String[] paths = destination.split("/");
        return Long.parseLong(paths[paths.length - 2]);
    }


}
