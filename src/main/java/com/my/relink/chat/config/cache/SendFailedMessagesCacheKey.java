package com.my.relink.chat.config.cache;

import lombok.Getter;

@Getter
public enum SendFailedMessagesCacheKey {
    TOPIC_PATH("topic_path"),
    MESSAGE("message")
    ;

    private final String value;

    SendFailedMessagesCacheKey(String value) {
        this.value = value;
    }
}
