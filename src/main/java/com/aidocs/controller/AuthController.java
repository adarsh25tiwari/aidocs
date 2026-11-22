package com.aidocs.controller;

import com.aidocs.dto.*;
import com.aidocs.entity.User;
import com.aidocs.respostory.UserRepository;
import com.aidocs.service.CustomUserDetail;
import com.aidocs.service.JwtService;
import com.aidocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        //do authentication and return token and user
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.username(),
                loginRequest.password()
        );

      Authentication authenticated =   authenticationManager.authenticate(authentication);
      User user = userRepository.findByUsername(loginRequest.username())
                      .orElseThrow(()-> new RuntimeException("User not found"));

      String token = jwtService.generateToken(new CustomUserDetail(user));

      var response = new LoginResponse(
              token,
              new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole()));

      return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(SignupRequest signupRequest) {
        UserDto userDto = userService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

}
