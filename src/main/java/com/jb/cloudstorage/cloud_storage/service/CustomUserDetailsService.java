package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.model.CustomUserDetails;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        UserEntity user = userOpt.orElseThrow(
                () -> new UsernameNotFoundException(String.format("user with username=%s is not found", username)));
        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword());
    }
}
