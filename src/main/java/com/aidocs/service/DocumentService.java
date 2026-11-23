package com.aidocs.service;

import com.aidocs.dto.DocumentMetadataDto;
import com.aidocs.dto.DocumentResponseDto;
import com.aidocs.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    DocumentResponseDto uploadAndProcess(MultipartFile file, User user);

    List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files, User user);

    List<DocumentMetadataDto> getAllDocuments();

    DocumentMetadataDto getDocumentById(UUID id,User user);

    void deleteDocument(UUID id,User  user);

    List<DocumentMetadataDto> getAllDocumentsByUser(User  user);
}
