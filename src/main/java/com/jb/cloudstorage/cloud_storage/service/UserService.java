package com.jb.cloudstorage.cloud_storage.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public String getUserMe() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
