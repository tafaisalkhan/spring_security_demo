package com.spring.security.demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class MethodTimingAspect {
    private static final Logger log = LoggerFactory.getLogger(MethodTimingAspect.class);
    @Value("${monitoring.method-enabled:true}")
    private boolean enabled;

    @Around("@annotation(com.spring.security.demo.annotation.TrackMethodTime)")
    public Object measure(ProceedingJoinPoint p) throws Throwable {
        if (!enabled) return p.proceed();
        long start = System.nanoTime();
        try {
            return p.proceed();
        } finally {
            log.info("METHOD {}.{} durationMs={}", p.getSignature().getDeclaringTypeName(), p.getSignature().getName(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }
}
