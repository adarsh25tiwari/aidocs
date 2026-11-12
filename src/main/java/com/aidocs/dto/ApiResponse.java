package com.aidocs.dto;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T>{
    private String message;
    private Boolean success;
    private LocalDateTime timestamp;
    private T data;
}
