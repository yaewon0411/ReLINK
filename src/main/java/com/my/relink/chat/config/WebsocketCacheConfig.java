package com.my.relink.chat.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.my.relink.chat.controller.dto.response.ChatMessageRespDto;
import com.my.relink.chat.service.dto.MessageTask;
import com.my.relink.domain.message.Message;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class WebsocketCacheConfig {

    @Bean
    public Cache<String, Map<String, Object>> sessionCache(){
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    @Bean
    public Cache<Long, String> userSessionCache(){
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    @Bean
    public Cache<Long, List<Map<String, Object>>> sendFailedMessagesCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(5_000)
                .build();
    }

    @Bean
    public Cache<String, Queue<MessageTask>> saveFailedMessageCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();
    }

    @Bean
    public Cache<String, MessageTask> processingSaveMessage() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    @Bean
    public Cache<String, MessageTask> retryFailedMessageQueue() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.DAYS)
                .maximumSize(1_000)
                .build();
    }
}
