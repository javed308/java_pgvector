package com.javed.java_pgvector.dto;

import java.util.List;
import java.util.Objects;

/** pageCount == 0 means that reliable page boundaries were NOT supplied. */
public record StructuredDocument(ParsedDocument source, List<TextBlock> blocks, int pageCount) {
    public StructuredDocument {
        Objects.requireNonNull(source, "source");
        blocks = List.copyOf(blocks);
        if (pageCount < 0) throw new IllegalArgumentException("Negative page count");
        for (TextBlock block : blocks) {
            if (block.pageNumber() != null && block.pageNumber() > pageCount) {
                throw new IllegalArgumentException("Block page exceeds page count");
            }
        }
    }
    public static StructuredDocument plain(ParsedDocument source) {
        return new StructuredDocument(source, List.of(TextBlock.text(source.text(), null)), 0);
    }
}
