package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import com.jb.cloudstorage.cloud_storage.dto.UserResponse;
import com.jb.cloudstorage.cloud_storage.exception.UsernameAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(SignUpRequest signUpRequest) {
        String username = signUpRequest.username();
        UserEntity userEntity = userRepository.findByUsername(username);

        if (userEntity != null) {
            throw new UsernameAlreadyExistsException(String.format("Username %s already exists, please create another one", username));
        }

        UserEntity newUserEntity = new UserEntity(username, passwordEncoder.encode(signUpRequest.password()));
        userRepository.save(newUserEntity);

        return new UserResponse(username);
    }
}
