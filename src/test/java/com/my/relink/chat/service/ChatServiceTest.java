package com.my.relink.chat.service;

import com.my.relink.chat.controller.dto.request.ChatImageReqDto;
import com.my.relink.chat.controller.dto.request.ChatMessageReqDto;
import com.my.relink.chat.controller.dto.response.ChatImageRespDto;
import com.my.relink.chat.controller.dto.response.ChatMessageRespDto;
import com.my.relink.chat.event.MessageSaveFailedEvent;
import com.my.relink.common.notification.NotificationPublisherService;
import com.my.relink.config.s3.S3Service;
import com.my.relink.controller.trade.dto.response.TradeCompletionRespDto;
import com.my.relink.domain.image.EntityType;
import com.my.relink.domain.image.Image;
import com.my.relink.domain.image.repository.ImageRepository;
import com.my.relink.domain.item.exchange.ExchangeItem;
import com.my.relink.domain.message.Message;
import com.my.relink.domain.message.repository.MessageRepository;
import com.my.relink.domain.notification.chat.ChatStatus;
import com.my.relink.domain.trade.Trade;
import com.my.relink.domain.trade.repository.dto.TradeWithOwnerItemNameDto;
import com.my.relink.domain.user.User;
import com.my.relink.ex.BusinessException;
import com.my.relink.ex.ErrorCode;
import com.my.relink.service.TradeService;
import com.my.relink.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private TradeService tradeService;
    @Mock
    private UserService userService;
    @Mock
    private S3Service s3Service;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private NotificationPublisherService notificationPublisherService;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationEventPublisher eventPublisher;



    @DisplayName("메시지 저장 테스트")
    @Nested
    class SaveMessage {
        private Long tradeId;
        private Long senderId;
        private ChatMessageReqDto chatMessageReqDto;
        private User sender;
        private Trade trade;
        private ExchangeItem ownerItem;
        private Message savedMessage;

        private TradeWithOwnerItemNameDto tradeWithItem;

        @BeforeEach
        void setUp() {
            tradeId = 1L;
            senderId = 2L;
            String messageContent = "안녕";

            chatMessageReqDto = new ChatMessageReqDto(messageContent, tradeId);

            sender = User.builder()
                    .id(senderId)
                    .nickname("riku")
                    .build();

            ownerItem = ExchangeItem.builder()
                    .id(3L)
                    .name("테스트 아이템")
                    .build();

            trade = Trade.builder()
                    .id(tradeId)
                    .ownerExchangeItem(ownerItem)
                    .build();

            savedMessage = Message.builder()
                    .id(4L)
                    .content(messageContent)
                    .trade(trade)
                    .user(sender)
                    .build();

            tradeWithItem = new TradeWithOwnerItemNameDto(trade, ownerItem.getName());

            when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
            when(clock.instant()).thenReturn(Instant.now());

            ReflectionTestUtils.setField(savedMessage, "createdAt", LocalDateTime.now());
        }

        @DisplayName("성공 케이스")
        @Nested
        class SuccessCase {
            @BeforeEach
            void setUp() {
                given(userService.findByIdOrFail(senderId))
                        .willReturn(sender);
                given(tradeService.findTradeWithOwnerItemName(tradeId))
                        .willReturn(tradeWithItem);
                given(messageRepository.save(any(Message.class)))
                        .willReturn(savedMessage);
                willDoNothing()
                        .given(notificationPublisherService)
                        .crateChatNotification(
                                senderId,
                                savedMessage.getContent(),
                                sender.getNickname(),
                                trade.getOwnerExchangeItem().getName(),
                                ChatStatus.NEW_CHAT
                        );
            }

            @Test
            @DisplayName("메시지를 저장하고 알림을 발행한다")
            void savesMessage_and_PublishesNotification() {
                ChatMessageRespDto result = chatService.saveMessage(tradeId, chatMessageReqDto, senderId);

                assertThat(result).isNotNull();
                assertThat(result.getContent()).isEqualTo(savedMessage.getContent());

                assertAll(() -> {
                    verify(userService).findByIdOrFail(senderId);
                    verify(tradeService).findTradeWithOwnerItemName(tradeId);
                    verify(messageRepository).save(any(Message.class));
                    verify(notificationPublisherService).crateChatNotification(
                            senderId,
                            savedMessage.getContent(),
                            sender.getNickname(),
                            trade.getOwnerExchangeItem().getName(),
                            ChatStatus.NEW_CHAT
                    );
                });
            }

//            @Test
//            @DisplayName("비동기 메시지 저장 실패 시, 이벤트가 발행된다")
//            void saveMessageAsync_publishes_MessageSaveFailedEvent() throws InterruptedException {
//                ChatMessageRespDto result = chatService.saveMessage(tradeId, chatMessageReqDto, senderId);
//
//                Thread.sleep(500);
//
//                verify(eventPublisher).publishEvent(argThat((Object event) -> {
//                    if (event instanceof MessageSaveFailedEvent) {
//                        MessageSaveFailedEvent failedEvent = (MessageSaveFailedEvent) event;
//                        return failedEvent.getMessage() != null;
//                    }
//                    return false;
//                }));
//            }

        }

        @DisplayName("실패 케이스")
        @Nested
        class FailureCase {

            @BeforeEach
            void setUp() {
                when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
                when(clock.instant()).thenReturn(Instant.now());
            }

            @Test
            @DisplayName("사용자를 찾을 수 없는 경우 예외를 던진다")
            void throwsException_whenUserNotFound() {
                given(userService.findByIdOrFail(senderId))
                        .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

                assertThatThrownBy(() ->
                        chatService.saveMessage(tradeId, chatMessageReqDto, senderId))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            }

            @Test
            @DisplayName("거래를 찾을 수 없는 경우 예외를 던진다")
            void throwsException_whenTradeNotFound() {
                given(userService.findByIdOrFail(senderId))
                        .willReturn(sender);
                given(tradeService.findTradeWithOwnerItemName(tradeId))
                        .willThrow(new BusinessException(ErrorCode.TRADE_NOT_FOUND));

                assertThatThrownBy(() ->
                        chatService.saveMessage(tradeId, chatMessageReqDto, senderId))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRADE_NOT_FOUND);
            }


        }
    }





    @DisplayName("채팅 이미지 저장 테스트")
    @Nested
    class SaveImageForChat {
        private Long tradeId;
        private ChatImageReqDto chatImageReqDto;
        private Trade trade;
        private String imageUrl;
        private MultipartFile mockImage;

        @BeforeEach
        void setUp() {
            tradeId = 1L;
            mockImage = new MockMultipartFile(
                    "image",
                    "test.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );
            chatImageReqDto = new ChatImageReqDto(mockImage);
            trade = Trade.builder()
                    .id(tradeId)
                    .build();
            imageUrl = "https://riku.com/image.jpg";
        }

        @DisplayName("성공 케이스")
        @Nested
        class SuccessCase {
            @BeforeEach
            void setUp() {
                given(tradeService.findByIdOrFail(tradeId))
                        .willReturn(trade);
                given(s3Service.upload(mockImage))
                        .willReturn(imageUrl);
                given(imageRepository.save(any(Image.class)))
                        .willReturn(Image.builder()
                                .imageUrl(imageUrl)
                                .entityType(EntityType.TRADE)
                                .entityId(tradeId)
                                .build());
            }

            @Test
            @DisplayName("채팅 이미지를 성공적으로 저장한다")
            void saveImageSuccessfully() {
                ChatImageRespDto result = chatService.saveImageForChat(tradeId, chatImageReqDto);

                assertThat(result).isNotNull();
                assertThat(result.getImageUrl()).isEqualTo(imageUrl);

                verify(tradeService).findByIdOrFail(tradeId);
                verify(s3Service).upload(mockImage);
                verify(imageRepository).save(any(Image.class));
            }
        }

        @DisplayName("실패 케이스")
        @Nested
        class FailureCase {

            @Test
            @DisplayName("Trade를 찾을 수 없는 경우 예외를 던진다")
            void throwException_whenTradeNotFound() {
                given(tradeService.findByIdOrFail(tradeId))
                        .willThrow(new BusinessException(ErrorCode.TRADE_NOT_FOUND));

                assertThatThrownBy(() ->
                        chatService.saveImageForChat(tradeId, chatImageReqDto))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRADE_NOT_FOUND);
            }

            @Test
            @DisplayName("S3 업로드 실패시 예외를 던진다")
            void throwExceptionWhenS3UploadFails() {
                given(tradeService.findByIdOrFail(tradeId))
                        .willReturn(trade);
                given(s3Service.upload(mockImage))
                        .willThrow(new RuntimeException("S3 업로드 실패"));

                assertThatThrownBy(() ->
                        chatService.saveImageForChat(tradeId, chatImageReqDto))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAIL_TO_SAVE_IMAGE);
            }

            @Test
            @DisplayName("이미지 저장 실패시 업로드된 S3 이미지를 삭제한다")
            void deleteS3ImageWhenImageSaveFails() {
                given(tradeService.findByIdOrFail(tradeId))
                        .willReturn(trade);
                given(s3Service.upload(mockImage))
                        .willReturn(imageUrl);
                given(imageRepository.save(any(Image.class)))
                        .willThrow(new RuntimeException("DB 저장 실패"));

                assertThatThrownBy(() ->
                        chatService.saveImageForChat(tradeId, chatImageReqDto))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FAIL_TO_SAVE_IMAGE);

                verify(s3Service).deleteImage(imageUrl);
            }
        }
    }

}