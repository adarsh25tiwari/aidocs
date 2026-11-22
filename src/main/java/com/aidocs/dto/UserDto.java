package com.aidocs.dto;

import com.aidocs.entity.Role;
import lombok.*;


public record UserDto(
        Long id,
        String username,
        String email,
        Role role) {


}
