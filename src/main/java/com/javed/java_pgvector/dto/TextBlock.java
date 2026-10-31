package com.javed.java_pgvector.dto;

import java.util.Objects;

/** Structure reported by a parser, not a guessed visual layout. */
public record TextBlock(Kind kind, String text, Integer pageNumber, int headingLevel) {
    public enum Kind { TEXT, HEADING, NAVIGATION, HEADER, FOOTER, CODE, TABLE }
    public TextBlock {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
        if (pageNumber != null && pageNumber < 1) throw new IllegalArgumentException("Invalid page");
        if (kind == Kind.HEADING ? headingLevel < 1 || headingLevel > 6 : headingLevel != 0) {
            throw new IllegalArgumentException("Only a heading can have a level from 1 to 6");
        }
    }
    public static TextBlock text(String text, Integer page) {
        return new TextBlock(Kind.TEXT, text, page, 0);
    }
}
