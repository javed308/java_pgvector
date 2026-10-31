package com.javed.java_pgvector.ingestion.parser;

import org.apache.tika.parser.html.DefaultHtmlMapper;
import org.apache.tika.parser.html.HtmlMapper;

import java.util.Locale;
import java.util.Set;

public final class CleaningHtmlMapper implements HtmlMapper {
    private static final Set<String> STRUCTURAL = Set.of("NAV", "HEADER", "FOOTER", "MAIN", "ARTICLE",
            "SECTION", "ASIDE", "DIV", "SPAN", "BR", "CODE");
    private final HtmlMapper delegate = DefaultHtmlMapper.INSTANCE;
    @Override public String mapSafeElement(String name) {
        return STRUCTURAL.contains(name.toUpperCase(Locale.ROOT))
                ? name.toLowerCase(Locale.ROOT) : delegate.mapSafeElement(name);
    }
    @Override public String mapSafeAttribute(String elementName, String attributeName) {
        return attributeName.equals("role") ? attributeName
                : delegate.mapSafeAttribute(elementName, attributeName);
    }
    @Override public boolean isDiscardElement(String name) { return delegate.isDiscardElement(name); }
}

