package com.aidocs.service;

import com.aidocs.entity.DocumentMetaData;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ParserService {
    public List<Document> parse(MultipartFile file);
}
