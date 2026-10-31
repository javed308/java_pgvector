package com.javed.java_pgvector.ingestion.api;

import com.javed.java_pgvector.ingestion.parser.DocumentParsingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;

@RestControllerAdvice(assignableTypes = {DocumentParsingController.class, DocumentCleaningController.class})
public class IngestionExceptionHandler {
    @ExceptionHandler(DocumentParsingException.class)
    ProblemDetail parsing(DocumentParsingException ex) {
        int status = switch (ex.reason()) {
            case EMPTY_FILE -> 400;
            case INPUT_TOO_LARGE, TEXT_TOO_LARGE -> 413;
            case UNSUPPORTED_TYPE -> 415;
            case NO_TEXT, PARSE_FAILED -> 422;
        };
        return problem(status, ex.reason().name(), ex.getMessage());
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail oversized(MaxUploadSizeExceededException ex) {
        return problem(413, "INPUT_TOO_LARGE", "The multipart upload exceeds the configured limit.");
    }
    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail missing(MissingServletRequestPartException ex) {
        return problem(400, "MISSING_FILE", "A multipart file part named 'file' is required.");
    }
    @ExceptionHandler(IOException.class)
    ProblemDetail readFailed(IOException ex) {
        return problem(500, "READ_FAILED", "The uploaded stream could not be read.");
    }
    private static ProblemDetail problem(int status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setProperty("code", code);
        return problem;
    }
}