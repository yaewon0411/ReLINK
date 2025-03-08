package com.my.relink.config.log;

import com.my.relink.config.log.context.HttpMDCContext;
import com.my.relink.config.log.context.SystemMDCContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class SystemLogAspect {

//
//    @Around("@within(org.springframework.stereotype.Service) || " +
//            "@within(org.springframework.stereotype.Repository)")
//    public Object logSystemOperation(ProceedingJoinPoint joinPoint) throws Throwable {
//        String className = joinPoint.getTarget().getClass().getSimpleName();
//        String methodName = joinPoint.getSignature().getName();
//
//        Map<String, String> httpContext = MDC.getCopyOfContextMap();
//        String existingRequestId = HttpMDCContext.getRequestId();
//
//        try (SystemMDCContext context = new SystemMDCContext()) {
//            MDC.clear();
//            context.putComponent(className);
//            context.putOperation(methodName);
//            context.putRequestId(existingRequestId);
//
//            long startTime = System.currentTimeMillis();
//            log.info("Started: {}.{}", className, methodName);
//
//            Object result = joinPoint.proceed();
//
//            long duration = System.currentTimeMillis() - startTime;
//            context.putDuration(duration);
//            log.info("Completed: {}.{}", className, methodName);
//
//            return result;
//        } catch (Exception e) {
//            log.error("Failed: {}.{}, error: {}", className, methodName, e.getMessage(), e);
//            throw e;
//        } finally {
//            if(httpContext != null) {
//                MDC.setContextMap(httpContext);
//            } else {
//                MDC.clear();
//            }
//        }
//    }
}
