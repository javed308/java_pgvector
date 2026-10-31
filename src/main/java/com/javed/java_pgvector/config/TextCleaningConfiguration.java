package com.javed.java_pgvector.config;

import com.javed.java_pgvector.ingestion.cleaner.CleaningOptions;
import com.javed.java_pgvector.ingestion.cleaner.DefaultTextCleaner;
import com.javed.java_pgvector.ingestion.cleaner.TextCleaner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;

@Configuration
public class TextCleaningConfiguration {
    @Bean
    TextCleaner textCleaner(
            @Value("${rag.cleaner.remove-marked-boilerplate:true}") boolean removeMarked,
            @Value("${rag.cleaner.detect-repeated-page-margins:false}") boolean detectRepeated,
            @Value("${rag.cleaner.known-margin-lines:}") String knownMargins) {
        // Semicolon-separated exact lines; no user-supplied regular expressions are executed.
        return new DefaultTextCleaner(new CleaningOptions(removeMarked, detectRepeated,
                new HashSet<>(Arrays.asList(knownMargins.split(";", -1)))));
    }
}
