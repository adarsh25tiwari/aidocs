package com.aidocs.controller;

import com.aidocs.dto.ApiResponse;
import com.aidocs.dto.DocumentResponseDto;
import com.aidocs.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        DocumentResponseDto document = documentService.uploadAndProcess(file);

        ApiResponse<DocumentResponseDto> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(document);
        response.setMessage("Document uploaded successfully");
        response.setTimestamp(LocalDateTime.now());

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
