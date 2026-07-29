package com.jb.cloudstorage.cloud_storage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForwardAllController {
    @GetMapping({
            "/files", "/files/**",
            "/login", "/login/**",
            "/registration", "/registration/**"
    })
    public String handleAllRequests() {
        return "forward:/index.html";
    }
}
