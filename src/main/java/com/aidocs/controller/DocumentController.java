package com.aidocs.controller;

import com.aidocs.dto.ApiResponse;
import com.aidocs.dto.DocumentMetadataDto;
import com.aidocs.dto.DocumentResponseDto;
import com.aidocs.entity.User;
import com.aidocs.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        // process document
        DocumentResponseDto document = documentService.uploadAndProcess(file,user);

        ApiResponse<DocumentResponseDto> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(document);
        response.setMessage("Document uploaded successfully");
        response.setTimestamp(LocalDateTime.now());

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {

        User user=(User)authentication.getPrincipal();

        List<DocumentResponseDto> responseDtos = documentService.uploadMultipleDocuments(files,user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<List<DocumentResponseDto>>
                                        builder()
                                .message("Document uploaded successfully")
                                .success(true)
                                .timestamp(LocalDateTime.now())
                                .data(responseDtos)
                                .build()
                );
    }

    //    list all uploaded documents of logged-in user
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<DocumentMetadataDto>>>
    getAllDocumentsOfLoggedInUser(Authentication authentication){

        User user = (User)  authentication.getPrincipal();

        java.util.List<DocumentMetadataDto> documents =documentService.getAllDocumentsByUser(user);
        return ResponseEntity.ok(
                ApiResponse.<List<DocumentMetadataDto>>
                                builder()
                        .message("All documents is here")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(documents)
                        .build()
        );
    }



    //    list all uploaded documents , for admin
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentMetadataDto>>> getAllDocuments(){
        java.util.List<DocumentMetadataDto> documents =documentService.getAllDocuments();
        return ResponseEntity.ok(
                ApiResponse.<List<DocumentMetadataDto>>
                                builder()
                        .message("All documents is here")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(documents)
                        .build()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentMetadataDto>> getDocumentById(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        DocumentMetadataDto document = documentService.getDocumentById(id,user);
        return ResponseEntity.ok(
                ApiResponse.<DocumentMetadataDto>
                                builder()
                        .message("Single document is here")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(document)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id,Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        documentService.deleteDocument(id, user);
        return ResponseEntity.ok(
                ApiResponse.<Void>
                                builder()
                        .message("Document deleted successfully")
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build()
        );
    }

}
