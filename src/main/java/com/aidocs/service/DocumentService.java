package com.aidocs.service;

import com.aidocs.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    public DocumentResponseDto uploadAndProcess(MultipartFile file);
}
