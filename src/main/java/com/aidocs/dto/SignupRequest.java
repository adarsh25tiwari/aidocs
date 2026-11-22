package com.aidocs.dto;

public record SignupRequest(
        String username,
        String password,
        String email
) {

}
