package com.aidocs.service;

import com.aidocs.configuration.AppConfig;
import com.aidocs.dto.CitationDto;
import com.aidocs.dto.SearchRequestDto;
import com.aidocs.dto.SearchResultDto;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);
    private final VectorStore  vectorStore;
    private final AppConfig  appConfig;
    private final ChatClient chatClient;


    @Override
    public SearchResultDto searchSimilarChunks(SearchRequestDto request) {
        java.util.List<Document> matchedDocs = retrieveRelevantDocuments(
                request.getQuery(),
                request.getDocumentId(),
                request.getTopK(),
                request.getSimilaritySearch());

        List<CitationDto> citations = matchedDocs.stream()
                .map(this::mapToCitation)
                .toList();

        return SearchResultDto.builder()
                .query(request.getQuery())
                .totalMatches(citations.size())
                .matches(citations)
                .build();
    }

    private List<Document> retrieveRelevantDocuments(@NotBlank(message = "Query cant be empty") String query,
                                                     UUID documentId,
                                                     Integer topK, Double similaritySearch) {

        int effectiveTopK = (topK != null && topK > 0) ? topK : appConfig.getRag().getTopK();

        double effectiveSimilarity = (similaritySearch != null) ? similaritySearch : appConfig.getRag().getSimilarityThreshold();

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                        .query(query)
                        .topK(effectiveTopK);

        if (effectiveSimilarity > 0.0) {
            searchRequestBuilder.similarityThreshold(effectiveSimilarity);
        }

        if(documentId != null) {
            FilterExpressionBuilder b = new  FilterExpressionBuilder();
            searchRequestBuilder.filterExpression(b.eq("documentId",documentId.toString()).build());
        }

        try{
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().build());
            return documents;

        }catch(Exception e){
            logger.error("Similarity search failed for query ;`{}`",query,e);
            return Collections.emptyList();


        }




    }

    private CitationDto mapToCitation(Document document) {

        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;
        if (meta.get("documentId") != null) {
            try{
                docId = UUID.fromString(meta.get("documentId").toString());
            }
            catch (Exception ignore) {

            }
        }
        Integer chunkIndex = null;
        if (meta.get("chunkIndex") instanceof Number n) {
            chunkIndex = n.intValue();
        }

        Integer pageNumber = null;
        if (meta.get("pageNumber") instanceof Number n) {
            pageNumber = n.intValue();
        }
        else if (meta.get("page_number") instanceof Number n) {
            pageNumber = n.intValue();
        }

        Double score = null;
        if (meta.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }
        return CitationDto.builder()
                .documentId(docId)
                .fileName((String) meta.getOrDefault("fileName", "Unknown"))
                .chunkIndex(chunkIndex)
                .pageNumber(pageNumber)
                .snippet(document.getText())
                .similarityScore(score)
                .metadata(meta).build();
    }
}
