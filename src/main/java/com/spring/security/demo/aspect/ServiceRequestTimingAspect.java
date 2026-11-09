package com.spring.security.demo.aspect;

import com.spring.security.demo.filter.RequestTimingFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class ServiceRequestTimingAspect {
    @Value("${monitoring.service-request-enabled:true}")
    private boolean enabled;

    @Around("@annotation(com.spring.security.demo.annotation.TrackServiceRequestTime)")
    public Object mark(ProceedingJoinPoint point) throws Throwable {
        if (enabled) {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servlet) {
                HttpServletRequest request = servlet.getRequest();
                if (request.getAttribute(RequestTimingFilter.START_ATTRIBUTE) != null)
                    request.setAttribute(RequestTimingFilter.SERVICE_ATTRIBUTE, Boolean.TRUE);
            }
        }
        return point.proceed();
    }
}
