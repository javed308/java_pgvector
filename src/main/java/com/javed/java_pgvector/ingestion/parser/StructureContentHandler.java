package com.javed.java_pgvector.ingestion.parser;

import com.javed.java_pgvector.dto.TextBlock;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

import static com.javed.java_pgvector.dto.TextBlock.Kind.*;

public class StructureContentHandler extends DefaultHandler {

    private static final Set<String> BOUNDARIES = Set.of("body", "div", "p", "h1", "h2", "h3", "h4",
            "h5", "h6", "nav", "header", "footer", "main", "article", "section", "aside",
            "ul", "ol", "li", "dl", "dt", "dd", "blockquote", "pre", "code", "table");
    private final boolean pdf;
    private final int maxCharacters;
    private final Deque<Element> stack = new ArrayDeque<>();
    private final List<TextBlock> blocks = new ArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private boolean body;
    private int pageCount;
    private Integer currentPage;
    private long characters;

    public StructureContentHandler(boolean pdf, int maxCharacters) {
        if (maxCharacters < 1) throw new IllegalArgumentException("maxCharacters must be positive");
        this.pdf = pdf;
        this.maxCharacters = maxCharacters;
    }
    public List<TextBlock> blocks() { return List.copyOf(blocks); }
    public int pageCount() { return pageCount; }

    @Override
    public void startElement(String uri, String local, String qualified, Attributes attributes) throws SAXException {
        String name = name(local, qualified);
        if (name.equals("body")) body = true;
        if (!body) return;
        if (stack.size() >= 256) throw new SAXException("Document nesting exceeds the lesson limit");
        boolean inLiteral = in("pre") || in("code") || in("table");
        if (!inLiteral && BOUNDARIES.contains(name)) flush();
        boolean isPage = pdf && name.equals("div") && token(attributes.getValue("class"), "page");
        if (isPage) { flush(); currentPage = ++pageCount; }
        boolean articleContext = in("article") || in("section") || in("main") || in("aside");
        String role = attributes.getValue("role");
        TextBlock.Kind kind = TEXT;
        if (name.equals("nav") || token(role, "navigation")) kind = NAVIGATION;
        else if (!articleContext && (name.equals("header") || token(role, "banner"))) kind = HEADER;
        else if (!articleContext && (name.equals("footer") || token(role, "contentinfo"))) kind = FOOTER;
        else if (name.matches("h[1-6]")) kind = HEADING;
        else if (name.equals("pre") || name.equals("code")) kind = CODE;
        else if (name.equals("table")) kind = TABLE;
        // Role-only regions on div/span also need a boundary before changing classification.
        if (!inLiteral && kind != TEXT && !BOUNDARIES.contains(name)) flush();
        stack.push(new Element(name, kind, isPage));
        if (name.equals("br")) append("\n");
    }

    @Override
    public void endElement(String uri, String local, String qualified) throws SAXException {
        if (!body) return;
        String name = name(local, qualified);
        Element current = stack.peek();
        if (current == null || !current.name().equals(name)) throw new SAXException("Unbalanced XHTML");
        boolean outerLiteral = stack.stream().skip(1)
                .anyMatch(e -> Set.of("pre", "code", "table").contains(e.name()));
        if (in("table") && (name.equals("td") || name.equals("th"))) append("\t");
        if (in("table") && name.equals("tr")) append("\n");
        if (!outerLiteral && (BOUNDARIES.contains(name) || current.kind() != TEXT)) flush();
        stack.pop();
        if (current.page()) currentPage = null;
        if (name.equals("body")) body = false;
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        if (body) append(new String(chars, start, length));
    }
    @Override
    public void ignorableWhitespace(char[] chars, int start, int length) throws SAXException {
        characters(chars, start, length);
    }
    @Override
    public void endDocument() throws SAXException { flush(); }

    private void append(String text) throws SAXException {
        characters += text.length();
        if (characters > maxCharacters) throw new LimitExceededException();
        buffer.append(text);
    }
    private void flush() throws SAXException {
        if (buffer.isEmpty()) return;
        String text = buffer.toString();
        buffer.setLength(0);
        if (text.isBlank()) return;
        if (blocks.size() >= 100_000) throw new LimitExceededException();
        TextBlock.Kind kind = TEXT;
        int level = 0;
        // Navigation is excluded as a region. Outside navigation, explicit headings win over headers.
        if (stack.stream().anyMatch(e -> e.kind() == NAVIGATION)) kind = NAVIGATION;
        else {
            Element heading = stack.stream().filter(e -> e.kind() == HEADING).findFirst().orElse(null);
            if (heading != null) { kind = HEADING; level = heading.name().charAt(1) - '0'; }
            else {
                Element margin = stack.stream().filter(e -> e.kind() == HEADER || e.kind() == FOOTER)
                        .findFirst().orElse(null);
                Element semantic = stack.stream().filter(e -> e.kind() != TEXT).findFirst().orElse(null);
                if (margin != null) kind = margin.kind();
                else if (semantic != null) kind = semantic.kind();
            }
        }
        blocks.add(new TextBlock(kind, text, currentPage, level));
    }
    private boolean in(String name) { return stack.stream().anyMatch(e -> e.name().equals(name)); }
    private static boolean token(String value, String token) {
        return value != null && Arrays.stream(value.toLowerCase(Locale.ROOT).split("\\s+"))
                .anyMatch(token::equals);
    }
    private static String name(String local, String qualified) {
        String value = local == null || local.isEmpty() ? qualified : local;
        int colon = value.indexOf(':');
        return value.substring(colon + 1).toLowerCase(Locale.ROOT);
    }
    private record Element(String name, TextBlock.Kind kind, boolean page) {}
    public static final class LimitExceededException extends SAXException {
        public LimitExceededException() { super("Structured text exceeds the configured limit"); }
    }
}
