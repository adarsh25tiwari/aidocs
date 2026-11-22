package com.aidocs.service;

import com.aidocs.dto.LoginRequest;
import com.aidocs.dto.LoginResponse;
import com.aidocs.dto.SignupRequest;
import com.aidocs.dto.UserDto;
import com.aidocs.entity.Role;
import com.aidocs.entity.User;
import com.aidocs.respostory.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto signup(SignupRequest signupRequest) {

        if(signupRequest.username().isBlank()){
            throw new IllegalArgumentException("Username is required");
        }
        else if(signupRequest.password().isBlank()){
            throw new IllegalArgumentException("Password is required");
        }

        User user = new User();
        user.setEmail(signupRequest.email());
        user.setPassword(passwordEncoder.encode(signupRequest.password()));
        user.setUsername(signupRequest.username());
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return new UserDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole());

    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        return null;
    }
}
