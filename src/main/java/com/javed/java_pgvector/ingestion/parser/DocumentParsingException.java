package com.javed.java_pgvector.ingestion.parser;

import java.util.Objects;

public class DocumentParsingException extends RuntimeException{
    public enum Reason {
        EMPTY_FILE, UNSUPPORTED_TYPE, INPUT_TOO_LARGE,
        TEXT_TOO_LARGE, NO_TEXT, PARSE_FAILED
    }

    private final Reason reason;

    public DocumentParsingException(Reason reason, String message) {
        this(reason, message, null);
    }

    public DocumentParsingException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
