package com.aidocs.service;

import com.aidocs.entity.DocumentMetaData;
import com.aidocs.exception.DocumentProcessingException;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ParserServiceImpl implements ParserService {

    @Override
    public List<Document> parse(MultipartFile file) {
        String fileName = file.getOriginalFilename()!= null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType()!= null ? file.getContentType() : "";

        try{
            Resource resource = new ByteArrayResource(file.getBytes()){
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            if(fileName.toLowerCase().endsWith(".pdf") || contentType.contains(".pdf")){
                return parsePdf(resource);
            }else{
                return genricParser(resource);
            }

        }catch(IOException ex){
            throw new  DocumentProcessingException("Could not read uploaded document..", ex);

        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse document..",e);
        }

    }


    private List<Document> genricParser(Resource resource) {
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        return tikaDocumentReader.read();
    }

    private List<Document> parsePdf(Resource resource) {
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageBottomMargin(0)
                .withPageTopMargin(0)
                .build();
        PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(resource,config);
        return pdfDocumentReader.read();
    }
}
