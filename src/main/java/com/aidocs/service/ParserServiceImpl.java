package com.aidocs.service;

import com.aidocs.entity.DocumentMetaData;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ParserServiceImpl implements ParserService {

    @Override
    public List<Document> parse(MultipartFile file) {
        return List.of();
    }
}
