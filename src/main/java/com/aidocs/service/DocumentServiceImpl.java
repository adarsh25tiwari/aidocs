package com.aidocs.service;

import com.aidocs.dto.DocumentResponseDto;
import com.aidocs.entity.DocumentMetaData;
import com.aidocs.entity.DocumentStatus;
import com.aidocs.respostory.DocumentMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService{

    private DocumentMetaDataRepository documentMetaDataRepository;
    private JdbcTemplate jdbcTemplate;
    private IngestService ingestService;
    private ParserService parserService;

    public DocumentServiceImpl(DocumentMetaDataRepository documentMetaDataRepository,
                               JdbcTemplate jdbcTemplate,
                               ParserService parserService,
                               IngestService ingestService) {
        this.documentMetaDataRepository = documentMetaDataRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.parserService = parserService;
        this.ingestService = ingestService;
    }
    private static final Logger logger  = LoggerFactory.getLogger(DocumentServiceImpl.class);

    @Override
    public DocumentResponseDto uploadAndProcess(MultipartFile file) {
    // save the document
       String fileName =  file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
       String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

       DocumentMetaData documentMetaData = DocumentMetaData
               .builder()
               .fileName(fileName)
               .contentType(contentType)
               .fileSize(file.getSize())
               .status(DocumentStatus.UPLOADING)
               .build();

       DocumentMetaData savedMetaData =  documentMetaDataRepository.save(documentMetaData);

    // parse the document
        List<Document> parsedDocument = parserService.parse(file);
    // ingest the document
       Integer chunksCreated = ingestService.ingest(savedMetaData,parsedDocument);

         return DocumentResponseDto.builder()
                 .id(savedMetaData.getId())
                 .fileName(savedMetaData.getFileName())
                 .chunksCreated(chunksCreated)
                 .fileSize(savedMetaData.getFileSize())
                 .status(savedMetaData.getStatus())
                 .message("document processed and indexed successfully..!")
                 .build();
    }
}
