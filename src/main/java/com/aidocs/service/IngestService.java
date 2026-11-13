package com.aidocs.service;

import com.aidocs.entity.DocumentMetaData;
import org.springframework.ai.document.Document;

import java.util.List;

public interface IngestService {
    public Integer ingest(DocumentMetaData documentMetaData,List<Document> parsedDocuments);
}
