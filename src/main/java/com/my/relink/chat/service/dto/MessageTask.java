package com.my.relink.chat.service.dto;

import com.my.relink.domain.message.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MessageTask {
    private Message message;
    private int currentTry;
}
