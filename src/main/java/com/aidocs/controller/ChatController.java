package com.aidocs.controller;

import com.aidocs.dto.*;
import com.aidocs.entity.User;
import com.aidocs.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatResponseDto>> askQuestion(
            @Valid @RequestBody ChatRequestDto requestDto,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ChatResponseDto chatResponseDto = ragService.askQuestion(requestDto,user.getId());
        return ResponseEntity.ok(
                ApiResponse.
                        <ChatResponseDto>
                        builder()
                        .success(true)
                        .message(null)
                        .data(chatResponseDto)
                        .timestamp(LocalDateTime.now())
                        .build()
        );

    }

    @PostMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String,Object>> streamQuestion(@Valid @RequestBody ChatRequestDto requestDto,
                                                   Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();

        return ragService.streamQuestionAnswer(requestDto,userId)
                .map(token -> Map.<String, Object>of("token", token))
                .concatWith(Flux.just(Map.<String, Object>of("done", true)))
                .onErrorResume(e -> Flux.just(Map.<String, Object>of(
                        "error", String.valueOf(e.getMessage()), "done", true)));

    }


    @PostMapping("/search/similarity")
    public ResponseEntity<ApiResponse<SearchResultDto>> searchSimilar(
            @Valid @RequestBody SearchRequestDto request,Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        SearchResultDto results = ragService.searchSimilarChunks(request,user.getId());
        return ResponseEntity.ok(
                ApiResponse.
                        <SearchResultDto>
                        builder()
                        .success(true)
                        .message(null)
                        .data(results)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

}
