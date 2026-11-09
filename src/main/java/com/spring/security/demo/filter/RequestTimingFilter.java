package com.spring.security.demo.filter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTimingFilter extends OncePerRequestFilter {
 public static final String START_ATTRIBUTE=RequestTimingFilter.class.getName()+".start";
 public static final String SERVICE_ATTRIBUTE=RequestTimingFilter.class.getName()+".serviceMatched";
 private static final Logger log=LoggerFactory.getLogger(RequestTimingFilter.class);
 private final AnnotatedRouteRegistry registry;
 @Value("${monitoring.controller-request-enabled:true}") private boolean controllerEnabled;
 @Value("${monitoring.service-request-enabled:true}") private boolean serviceEnabled;
 public RequestTimingFilter(AnnotatedRouteRegistry registry){this.registry=registry;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){return !controllerEnabled && !serviceEnabled;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
   boolean controllerMatched=controllerEnabled && registry.matches(request);
   long start=System.nanoTime();
   request.setAttribute(START_ATTRIBUTE,start);
   try {chain.doFilter(request,response);}
   finally {
     boolean serviceMatched=serviceEnabled && Boolean.TRUE.equals(request.getAttribute(SERVICE_ATTRIBUTE));
     if(controllerMatched || serviceMatched) {
       long duration=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
       log.info("REQUEST method={} path={} status={} durationMs={} controllerMatched={} serviceMatched={}",request.getMethod(),request.getRequestURI(),response.getStatus(),duration,controllerMatched,serviceMatched);
     }
   }
 }
}
