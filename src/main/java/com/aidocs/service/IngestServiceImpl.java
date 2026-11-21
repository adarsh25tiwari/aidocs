package com.aidocs.service;

import com.aidocs.configuration.AppConfig;
import com.aidocs.entity.DocumentMetaData;
import com.aidocs.entity.DocumentStatus;
import com.aidocs.exception.DocumentProcessingException;
import com.aidocs.respostory.DocumentMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class IngestServiceImpl implements IngestService{

    private static Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    private final VectorStore vectorStore;
    private final DocumentMetaDataRepository documentMetaDataRepository;
    private final  AppConfig appConfig;

    public IngestServiceImpl(VectorStore vectorStore,
                             DocumentMetaDataRepository documentMetaDataRepository,
                             AppConfig appConfig){
        this.vectorStore = vectorStore;
        this.documentMetaDataRepository = documentMetaDataRepository;
        this.appConfig = appConfig;
    }

    @Override
    public Integer ingest(DocumentMetaData documentMetaData, List<Document> parsedDocuments) {
        logger.info("Ingesting document [id={},name={},pages={}",
                documentMetaData.getId(),documentMetaData.getFileName(),documentMetaData.getFileSize());

            try{
                documentMetaData.setStatus(DocumentStatus.PROCESSING);
                documentMetaData.setTotalPages(parsedDocuments.size());

                // Text chunking using Token-text splitter
                TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                        .withChunkSize(appConfig.getRag().getChunkSize())
                        .withMaxNumChunks(appConfig.getRag().getMaxNumChunks())
                        .withMinChunkSizeChars(appConfig.getRag().getMinChunkSizeChars())
                        .withMinChunkLengthToEmbed(appConfig.getRag().getMinChunkLengthToEmbed())
                        .withMaxNumChunks(appConfig.getRag().getMaxNumChunks())
                        .withKeepSeparator(true)
                        .build();

                List<Document> chunks = tokenTextSplitter.apply(parsedDocuments);

                if(chunks.isEmpty()){
                    documentMetaData.setStatus(DocumentStatus.FAILED);
                    documentMetaDataRepository.save(documentMetaData);
                    return 0;
                }

                // Enrich metadata on each chunks
                List<Document> enrichedChunks = new ArrayList<>();
                for(int i = 0; i <  chunks.size(); i++){
                    Document chunk = chunks.get(i);
                    HashMap<String,Object> enrichedMetaData =  new HashMap<>(chunk.getMetadata());
                    enrichedMetaData.put("documentId", documentMetaData.getId().toString());
                    enrichedMetaData.put("fileName", documentMetaData.getFileName());
                    enrichedMetaData.put("contentType", documentMetaData.getContentType());
                    enrichedMetaData.put("chunkIndex", i);

                    // Preserve or calculate page number if available
                    Object pageNumber = chunk.getMetadata().get("page_number");
                    if (pageNumber == null) {
                        pageNumber = chunk.getMetadata().get("pageNumber");
                    }
                    if (pageNumber != null) {
                        enrichedMetaData.put("pageNumber", pageNumber);
                    }
                    Document enrichedDoc = new Document(chunk.getText(), enrichedMetaData);
                    enrichedChunks.add(enrichedDoc);
                }

                //write chunks and embedding to pg vector
                logger.info("Writing {} vector chunks to PgVectorStore for document: {}",
                        enrichedChunks.size(), documentMetaData.getFileName());

                vectorStore.add(enrichedChunks);

                //Update document status to index
                documentMetaData.setStatus(DocumentStatus.INDEXED);
                documentMetaData.setTotalChunks(enrichedChunks.size());
                documentMetaData.setErrorMessage(null);
                documentMetaDataRepository.save(documentMetaData);

                logger.info("Successfully indexed document [id={}, name={}, chunks={}]",
                        documentMetaData.getId(),documentMetaData.getFileName(), enrichedChunks.size());

                return enrichedChunks.size();

            }

            catch(Exception ex){
                logger.error("Failed to ingest document into vector store: {}", documentMetaData.getFileName(),ex);
                documentMetaData.setStatus(DocumentStatus.FAILED);
                documentMetaData.setErrorMessage(ex.getMessage());
                documentMetaDataRepository.save(documentMetaData);
                throw new DocumentProcessingException("Failed to index document: " + ex.getMessage(), ex);

            }

    }
}
