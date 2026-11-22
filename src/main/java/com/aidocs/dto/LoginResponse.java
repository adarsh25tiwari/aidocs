package com.aidocs.dto;

public record LoginResponse(
        String accessToken,
        UserDto user
) {

}
