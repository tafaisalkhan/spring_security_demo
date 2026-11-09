package com.spring.security.demo.filter;

import com.spring.security.demo.annotation.TrackControllerRequestTime;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.http.server.PathContainer;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

@Component
public class AnnotatedRouteRegistry implements SmartInitializingSingleton {
    private final RequestMappingHandlerMapping mappings;
    private volatile List<Route> routes = List.of();

    public AnnotatedRouteRegistry(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        this.mappings = mappings;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<Route> found = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.getHandlerMethods().entrySet()) {
            if (!entry.getValue().hasMethodAnnotation(TrackControllerRequestTime.class)) continue;
            RequestMappingInfo info = entry.getKey();
            if (info.getPathPatternsCondition() == null)
                throw new IllegalStateException("PathPatternParser required for monitoring");
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (PathPattern pattern : info.getPathPatternsCondition().getPatterns())
                found.add(new Route(pattern, Set.copyOf(methods)));
        }
        routes = List.copyOf(found);
    }

    public boolean matches(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        PathContainer container = PathContainer.parsePath(path);
        return routes.stream().anyMatch(r -> (r.methods().isEmpty() || r.methods().stream().anyMatch(m -> m.name().equalsIgnoreCase(request.getMethod()))) && r.pattern().matches(container));
    }

    private record Route(PathPattern pattern, Set<RequestMethod> methods) {
    }
}
