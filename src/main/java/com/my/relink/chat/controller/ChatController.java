package com.my.relink.chat.controller;

import com.my.relink.chat.config.ChatPrincipal;
import com.my.relink.chat.controller.dto.request.ChatImageReqDto;
import com.my.relink.chat.controller.dto.request.ChatMessageReqDto;
import com.my.relink.chat.controller.dto.response.ChatImageRespDto;
import com.my.relink.chat.controller.dto.response.ChatMessageRespDto;
import com.my.relink.chat.service.ChatRetryService;
import com.my.relink.chat.service.ChatService;
import com.my.relink.util.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;
    private final ChatRetryService chatRetryService;
    private final static String TOPIC_PATH = "/topic/chats/";

    @MessageMapping("/chats/{tradeId}/message")
    public void handleMessage(@DestinationVariable("tradeId") Long tradeId,
                              @Payload ChatMessageReqDto chatMessageReqDto,
                              Principal principal) {
        ChatMessageRespDto response = chatService.saveMessage(
                        tradeId,
                        chatMessageReqDto,
                        ((ChatPrincipal)principal).getUserId());
        try {
            messagingTemplate.convertAndSend(TOPIC_PATH + tradeId, response);
        } catch (MessagingException e){
            chatRetryService.saveSendFailedMessage(TOPIC_PATH, response, tradeId);
        }
    }

    @PostMapping(value = "/chats/{tradeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<ChatImageRespDto>> saveImageForChat(@PathVariable("tradeId") Long tradeId,
                                                                        @ModelAttribute @Valid ChatImageReqDto chatImageReqDto){
        return ResponseEntity.ok(ApiResult.success(chatService.saveImageForChat(tradeId, chatImageReqDto)));
    }

}
