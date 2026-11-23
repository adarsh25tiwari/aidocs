package com.aidocs.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppConfig {

    private RagConfig rag =  new RagConfig();
    private CorsConfig cors =  new CorsConfig();
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagConfig{
        private int chunkSize = 700;
        private int minChunkSizeChars = 350;
        private int minChunkLengthToEmbed = 5;
        private int maxNumChunks = 10000;
        private int topK = 5;
        private double similarityThreshold = 0.0;

        // Used by chat when NO single document is selected. Chunks scoring below this are ignored,
        // so "hi" / general questions are answered by the LLM instead of being forced onto document text.
        private double chatSimilarityThreshold = 0.5;


    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorsConfig{
        private String allowedOrigins = "*";
        private String allowedHeaders = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";


    }

}
