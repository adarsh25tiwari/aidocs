package com.aidocs.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRequestDto {

    @NotBlank(message = "Query cant be empty")
    private String query;

    private UUID documentId;

    private  Integer topK;

    private  Double similaritySearch;
}
