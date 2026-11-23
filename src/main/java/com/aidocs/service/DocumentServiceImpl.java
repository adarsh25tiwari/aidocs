package com.aidocs.service;

import com.aidocs.dto.DocumentMetadataDto;
import com.aidocs.dto.DocumentResponseDto;
import com.aidocs.entity.DocumentMetaData;
import com.aidocs.entity.DocumentStatus;
import com.aidocs.entity.User;
import com.aidocs.exception.DocumentProcessingException;
import com.aidocs.exception.ResourceNotFoundException;
import com.aidocs.respostory.DocumentMetaDataRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService{

    private static final Logger log =  LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentMetaDataRepository documentMetaDataRepository;
    private final  JdbcTemplate jdbcTemplate;
    private final IngestService ingestService;
    private final ParserService parserService;
    private final ModelMapper modelMapper;

    public DocumentServiceImpl(DocumentMetaDataRepository documentMetaDataRepository,
                               JdbcTemplate jdbcTemplate,
                               ParserService parserService,
                               IngestService ingestService,
                               ModelMapper modelMapper) {
        this.documentMetaDataRepository = documentMetaDataRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.parserService = parserService;
        this.ingestService = ingestService;
        this.modelMapper = modelMapper;
    }

    @Override
    public DocumentResponseDto uploadAndProcess(MultipartFile file, User user) {
    // save the document
       String fileName =  file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
       String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

       DocumentMetaData documentMetaData = DocumentMetaData
               .builder()
               .fileName(fileName)
               .contentType(contentType)
               .fileSize(file.getSize())
               .status(DocumentStatus.UPLOADING)
               .user(user)
               .creationDate(LocalDateTime.now())
               .build();

       DocumentMetaData savedMetaData =  documentMetaDataRepository.save(documentMetaData);
       Integer chunksCreated = 0;
       List<Document> parsedDocument = null;
       try{
           // parse the document
            parsedDocument = parserService.parse(file);
           // ingest the document
            chunksCreated = ingestService.ingest(savedMetaData,parsedDocument);
       }
       catch(RuntimeException e){
           documentMetaDataRepository.delete(savedMetaData);
           throw e;
       }


         return DocumentResponseDto.builder()
                 .id(savedMetaData.getId())
                 .fileName(savedMetaData.getFileName())
                 .chunksCreated(chunksCreated)
                 .fileSize(savedMetaData.getFileSize())
                 .status(savedMetaData.getStatus())
                 .userId(savedMetaData.getUser().getId())
                 .message("document processed and indexed successfully..!")
                 .build();
    }

    @Override
    public List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files,User user) {

        List<DocumentResponseDto> responseDtos = new ArrayList<>();
        for (MultipartFile file : files) {
            DocumentResponseDto result = this.uploadAndProcess(file,user);
            responseDtos.add(result);
        }

        return responseDtos;
    }

    // It will be for admin future impl
    @Override
    public List<DocumentMetadataDto> getAllDocuments() {
        List<DocumentMetaData> allDocuments = documentMetaDataRepository.findAllByOrderByCreationDateDesc();
        return allDocuments.stream()
                .map(documentMetaData  -> modelMapper.map(documentMetaData, DocumentMetadataDto.class))
                .toList();
    }

    // User to get all his documents
    public List<DocumentMetadataDto> getAllDocumentsByUser(User  user) {
        List<DocumentMetaData> documents = documentMetaDataRepository.findByUserOrderByCreationDateDesc(user);
        return documents.stream()
                .map(documentMetaData  -> modelMapper.map(documentMetaData, DocumentMetadataDto.class))
                .toList();
    }

    @Override
    public DocumentMetadataDto getDocumentById(UUID id) {
        DocumentMetaData documentMetaData = documentMetaDataRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));
        return modelMapper.map(documentMetaData, DocumentMetadataDto.class);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {

        DocumentMetaData documentMetaData = documentMetaDataRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));

        documentMetaDataRepository.delete(documentMetaData);
        //delete the vector entries
        try {
            String deleteVectorsSql = "DELETE FROM vector_store WHERE metadata->>'documentId' = ?";
            int deletedCount = jdbcTemplate.update(deleteVectorsSql, id.toString());
            log.info("Deleted {} vector chunks for document id {} ", deletedCount, id);

        } catch (Exception e) {
            log.warn("cloud not delete vectors from vector store directly: {}", e.getMessage());
        }

    }
}
