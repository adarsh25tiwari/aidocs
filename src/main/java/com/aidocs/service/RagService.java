package com.aidocs.service;

import com.aidocs.dto.SearchRequestDto;
import com.aidocs.dto.SearchResultDto;
import org.springframework.ai.document.Document;

public interface RagService {
    SearchResultDto searchSimilarChunks(SearchRequestDto request);


}
