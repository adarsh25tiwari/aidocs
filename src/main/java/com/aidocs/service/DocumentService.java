package com.aidocs.service;

import com.aidocs.dto.DocumentMetadataDto;
import com.aidocs.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    public DocumentResponseDto uploadAndProcess(MultipartFile file);

    List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files);

    List<DocumentMetadataDto> getAllDocuments();

    DocumentMetadataDto getDocumentById(UUID id);

    void deleteDocument(UUID id);
}
