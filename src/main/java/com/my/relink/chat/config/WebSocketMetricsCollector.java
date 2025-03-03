package com.my.relink.chat.config;

import com.my.relink.chat.config.metric.TaskMetric;
import com.my.relink.chat.config.metric.WebSocketMetricsHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.DoubleSummaryStatistics;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMetricsCollector {
    private final WebSocketSessionManager sessionManager;

    private final ThreadPoolTaskExecutor webSocketTaskExecutor;
    private long lastCompletedTasks = 0;
    private final WebSocketMetricsHolder metricsHolder;

    //@Scheduled(fixedRate = 1000)
    public void logMetrics() {
        ThreadPoolExecutor tpe = webSocketTaskExecutor.getThreadPoolExecutor();
        long completedTasks = tpe.getCompletedTaskCount();

        //초당 처리량 계산
        long tasksDelta = completedTasks - lastCompletedTasks;
        lastCompletedTasks = completedTasks;

        DoubleSummaryStatistics waitStats = metricsHolder.getRecentMetrics().stream()
                .mapToDouble(TaskMetric::getWaitDuration)
                .summaryStatistics();

        DoubleSummaryStatistics processStats = metricsHolder.getRecentMetrics().stream()
                .mapToDouble(TaskMetric::getProcessDuration)
                .summaryStatistics();

        log.info("WebSocket Metrics - " +
                        "활성 스레드: {}, " +
                        "완료된 작업: {}, " +
                        "큐 크기: {}, " +
                        "큐 남은 용량: {}, " +
                        "초당 처리량: {}, " +
                        "평균 대기시간: {}ms, " +
                        "최대 대기시간: {}ms, " +
                        "평균 처리시간: {}ms, " +
                        "최대 처리시간: {}ms",
                webSocketTaskExecutor.getActiveCount(),
                completedTasks,
                webSocketTaskExecutor.getQueueSize(),
                tpe.getQueue().remainingCapacity(),
                tasksDelta,
                String.format("%.2f", waitStats.getAverage()),
                waitStats.getMax(),
                String.format("%.2f", processStats.getAverage()),
                processStats.getMax());
    }


}