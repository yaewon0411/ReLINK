package com.my.relink.chat.event;

import com.my.relink.domain.message.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessageSaveFailedEvent extends ApplicationEvent {
    private final Message message;

    public MessageSaveFailedEvent(Message message) {
        super(message);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
