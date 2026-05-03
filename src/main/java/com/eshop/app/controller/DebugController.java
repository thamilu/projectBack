package com.eshop.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Value("${server.max-http-header-size:unknown}")
    private String maxHttpHeaderSize;

    @Value("${server.tomcat.max-http-header-size:unknown}")
    private String tomcatMaxHttpHeaderSize;

    @GetMapping("/header-config")
    public String getHeaderConfig() {
        return "server.max-http-header-size: " + maxHttpHeaderSize + "\n" +
               "server.tomcat.max-http-header-size: " + tomcatMaxHttpHeaderSize;
    }
}
