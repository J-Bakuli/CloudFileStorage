package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import com.jb.cloudstorage.cloud_storage.dto.UserResponse;
import com.jb.cloudstorage.cloud_storage.exception.InvalidCredentialsException;
import com.jb.cloudstorage.cloud_storage.exception.UsernameAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Transactional
    public UserResponse register(SignUpRequest signUpRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = signUpRequest.username().trim();
        UserEntity userEntity = userRepository.findByUsername(username);

        if (userEntity != null) {
            throw new UsernameAlreadyExistsException(String.format("Username %s already exists, please create another one", username));
        }

        UserEntity newUserEntity = new UserEntity(username, passwordEncoder.encode(signUpRequest.password()));
        userRepository.save(newUserEntity);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        signUpRequest.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        log.info("Successful sign-up with username={}", username);
        return new UserResponse(username);
    }

    public UserResponse login(SignInRequest signInRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = signInRequest.username().trim();
        UserEntity userEntity = userRepository.findByUsername(username);

        if (userEntity == null || !passwordEncoder.matches(signInRequest.password(), userEntity.getPassword())) {
            log.warn("Sign-in failed for username={}", username);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        signInRequest.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        log.info("Successful sign-in with username={}", username);
        return new UserResponse(username);
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        log.debug("Session invalidated on sign-out");
    }
}
