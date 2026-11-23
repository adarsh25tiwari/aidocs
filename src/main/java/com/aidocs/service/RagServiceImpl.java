package com.aidocs.service;

import com.aidocs.configuration.AppConfig;
import com.aidocs.dto.*;
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
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);
    private final VectorStore  vectorStore;
    private final AppConfig  appConfig;
    private final ChatClient chatClient;


    @Override
    public SearchResultDto searchSimilarChunks(SearchRequestDto request,Long  userId) {
        java.util.List<Document> matchedDocs = retrieveRelevantDocuments(
                request.getQuery(),
                request.getDocumentId(),
                userId,
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
                                                     Long  userId,
                                                     Integer topK, Double similaritySearch) {

        int effectiveTopK = (topK != null && topK > 0) ? topK : appConfig.getRag().getTopK();

        double effectiveSimilarity = (similaritySearch != null) ? similaritySearch : appConfig.getRag().getSimilarityThreshold();

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                        .query(query)
                        .topK(effectiveTopK);

        if (effectiveSimilarity > 0.0) {
            searchRequestBuilder.similarityThreshold(effectiveSimilarity);
        }

        // Only ever search the logged-in user's own chunks (stored as metadata "userId" during ingest)
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = b.eq("userId", userId);
        if (documentId != null) {
            filter = b.and(filter, b.eq("documentId", documentId.toString()));
        }
        searchRequestBuilder.filterExpression(filter.build());

        try{
            List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
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

    /**
     * Chat threshold:
     *  - caller supplied minSimilarity -> use it
     *  - a single document is selected -> 0 (always give the model that document's best chunks)
     *  - all documents -> app.rag.chat-similarity-threshold, so unrelated / general messages
     *    (greetings, maths, coding...) get NO document context and are answered as a normal chatbot.
     */
    private double chatThreshold(ChatRequestDto request) {
        if (request.getMinSimilarity() != null) {
            return request.getMinSimilarity();
        }
        return request.getDocumentId() != null ? 0.0 : appConfig.getRag().getChatSimilarityThreshold();
    }



    public ChatResponseDto askQuestion(ChatRequestDto request,Long userId) {
        long startTime = System.currentTimeMillis();

        List<Document> similarDocuments = retrieveRelevantDocuments(
                request.getQuestion(),
                request.getDocumentId(),
                userId,
                request.getTopK(),
                request.getMinSimilarity()
        );


        List<CitationDto> citationDtos = similarDocuments.stream()
                .map(this::mapToCitation)
                .toList();

        String contextText = buildContextString(similarDocuments);
        String prompt = buildPrompt(request.getQuestion(), contextText);

        String answer = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;

        ChatResponseDto responseDto = ChatResponseDto.builder()
                .answer(answer)
                .conversationId(request.getConversationId() !=null ?  request.getConversationId().toString() : UUID.randomUUID().toString())
                .citations(citationDtos)
                .responseTimeMs(responseTime)
                .build();

        return responseDto;
    }

    private String buildPrompt(@NotBlank(message = "Question cannot be empty") String question, String contextText) {

        if (contextText != null && !contextText.isBlank()) {
            return String.format("""
                           Document Context:
                           ---------------------
                           %s
                           ---------------------                                        
                           User Message / Question: %s             
                           Instructions:
                            - If the question relates to the document context above, answer from that context and mention the source file / page.
                            - If the context is not relevant to the question, ignore it and answer helpfully from your general knowledge.
                            - Never reply with "no chunks / no context found"; always give the user a useful answer.
                    
                    """, contextText, question);
        } else {
            return String.format("""
                       User Message / Question:
                       %s                         
                       Instructions:
                         - No uploaded document matched this message. Respond helpfully, accurately and conversationally using your general knowledge.
                         - Greetings and small talk are welcome: reply naturally.
                         - If the user is clearly asking about the contents of their own documents, say briefly that you could not find that in their uploaded documents, then help as best you can.
                         - Never reply with "no chunks / no context found".
                    """, question);
        }


    }

    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }

        return similarDocuments.stream().map(doc -> {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");
            return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
        }).collect(Collectors.joining("\n\n---\n\n"));

    }

    public Flux<String> streamQuestionAnswer(ChatRequestDto requestDto,Long userId) {
        List<Document> relevantDocuments = retrieveRelevantDocuments(
                requestDto.getQuestion(),
                requestDto.getDocumentId(),
                userId,
                requestDto.getTopK(),
                requestDto.getMinSimilarity()
        );

        String contextText = buildContextString(relevantDocuments);
        String userPrompt = buildPrompt(requestDto.getQuestion(), contextText);

        return chatClient.prompt()
                .user(userPrompt)
                .stream()
                .content();
    }
}
