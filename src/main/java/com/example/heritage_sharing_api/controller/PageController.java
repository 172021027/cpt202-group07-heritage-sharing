package com.example.heritage_sharing_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {

    @GetMapping("/api/health")
    @ResponseBody
    public String health() {
        return "{\"status\":\"ok\"}";
    }
}

