package com.spring.security.demo.web;

import com.spring.security.demo.annotation.*;
import org.springframework.stereotype.Service;

@Service
public class DemoService {
    @TrackServiceRequestTime
    public String serviceRequest() throws InterruptedException {
        Thread.sleep(90);
        return "service request measured";
    }

    @TrackMethodTime
    public String method() throws InterruptedException {
        Thread.sleep(60);
        return "method measured";
    }

    @TrackServiceRequestTime
    @TrackMethodTime
    public String both() throws InterruptedException {
        Thread.sleep(110);
        return "service request and method measured";
    }
}
