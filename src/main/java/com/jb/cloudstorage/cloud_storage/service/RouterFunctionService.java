package com.jb.cloudstorage.cloud_storage.service;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.function.RouterFunction;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Service
public class RouterFunctionService {
    @Bean
    RouterFunction spaRouter() {
        ClassPathResource index = new ClassPathResource("static/index.html");
        return route().resource(path("/"), index).build();
    }
}
