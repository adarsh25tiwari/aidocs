package com.aidocs.service;

import com.aidocs.entity.DocumentMetaData;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestServiceImpl implements IngestService{

    @Override
    public Integer ingest(DocumentMetaData documentMetaData, List<Document> parsedDocuments) {
        return 0;
    }
}
