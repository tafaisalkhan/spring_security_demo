package com.spring.security.demo.web;

import com.spring.security.demo.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class DemoController {
    private final DemoService service;

    public DemoController(DemoService service) {
        this.service = service;
    }

    @GetMapping("/demo/controller")
    @TrackControllerRequestTime
    public String controller() throws InterruptedException {
        Thread.sleep(80);
        return "controller request measured";
    }

    @GetMapping("/demo/service")
    public String service() throws InterruptedException {
        return service.serviceRequest();
    }

    @GetMapping("/demo/method")
    public String method() throws InterruptedException {
        return service.method();
    }

    @GetMapping("/demo/all")
    @TrackControllerRequestTime
    public String all() throws InterruptedException {
        return service.both();
    }

    @GetMapping("/demo/plain")
    public String plain() {
        return "no monitoring";
    }

    @GetMapping("/demo/public")
    @TrackControllerRequestTime
    public String publicEndpoint() {
        return "public request measured";
    }
}
