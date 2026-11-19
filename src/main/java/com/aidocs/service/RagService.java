package com.aidocs.service;

import com.aidocs.dto.ChatRequestDto;
import com.aidocs.dto.ChatResponseDto;
import com.aidocs.dto.SearchRequestDto;
import com.aidocs.dto.SearchResultDto;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

public interface RagService {
    SearchResultDto searchSimilarChunks(SearchRequestDto request);
    Flux<String> streamQuestionAnswer(ChatRequestDto requestDto);
    ChatResponseDto askQuestion(ChatRequestDto request);

}
