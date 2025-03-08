package com.my.relink.chat.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.my.relink.chat.service.dto.MessageTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebsocketCacheConfig {

    @Bean(name=  "sessionCache")
    public Cache<String, Map<String, Object>> sessionCache(){
        return Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Bean(name = "userSessionCache")
    public Cache<Long, String> userSessionCache(){
        return Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Bean(name = "sendFailedMessagesCache")
    public Cache<Long, List<Map<String, Object>>> sendFailedMessagesCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(5_000)
                .build();
    }

    @Bean(name = "saveFailedMessageCache")
    public Cache<String, Queue<MessageTask>> saveFailedMessageCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();
    }

//    @Bean
//    @Qualifier("processingSaveMessage")
//    public Cache<String, MessageTask> processingSaveMessage() {
//        return Caffeine.newBuilder()
//                .expireAfterWrite(30, TimeUnit.MINUTES)
//                .maximumSize(10_000)
//                .build();
//    }

    @Bean
    @Qualifier("retryFailedMessage")
    public Cache<String, MessageTask> retryFailedMessage() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.DAYS)
                .maximumSize(1_000)
                .build();
    }
}
