package com.my.relink.chat.event;

import lombok.Getter;

@Getter
public class MessageSendRetryEvent {
    private final Long userId;

    public MessageSendRetryEvent(Long userId) {
        this.userId = userId;
    }
}
