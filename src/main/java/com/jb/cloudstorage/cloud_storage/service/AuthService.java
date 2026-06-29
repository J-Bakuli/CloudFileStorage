package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import com.jb.cloudstorage.cloud_storage.dto.UserResponse;
import com.jb.cloudstorage.cloud_storage.exception.InvalidCredentialsException;
import com.jb.cloudstorage.cloud_storage.exception.UsernameAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(SignUpRequest signUpRequest) {
        String username = signUpRequest.username();
        UserEntity userEntity = userRepository.findByUsername(username);

        if (userEntity != null) {
            throw new UsernameAlreadyExistsException(String.format("Username %s already exists, please create another one", username));
        }

        UserEntity newUserEntity = new UserEntity(username, passwordEncoder.encode(signUpRequest.password()));
        userRepository.save(newUserEntity);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signUpRequest.username(),
                        signUpRequest.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return new UserResponse(username);
    }

    public UserResponse login(SignInRequest signInRequest) {
        String username = signInRequest.username();
        UserEntity userEntity = userRepository.findByUsername(username);

        if (userEntity == null || !passwordEncoder.matches(signInRequest.password(), userEntity.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signInRequest.username(),
                        signInRequest.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return new UserResponse(username);
    }
}
