package com.javed.java_pgvector.ingestion.cleaner;

import com.javed.java_pgvector.dto.CleanedDocument;
import com.javed.java_pgvector.dto.CleanedDocument.CleaningReport;
import com.javed.java_pgvector.dto.CleanedDocument.RemovedText;
import com.javed.java_pgvector.dto.StructuredDocument;
import com.javed.java_pgvector.dto.TextBlock;

import java.util.*;
import java.util.regex.Pattern;

import static com.javed.java_pgvector.dto.TextBlock.Kind.*;

/** Deterministic preparation of PROSE; it is not a semantic document-understanding model. */
public final class DefaultTextCleaner implements TextCleaner {
    private static final Pattern HORIZONTAL = Pattern.compile("\\h+");
    private static final Pattern ATX_HEADING = Pattern.compile("^#{1,6} +\\S.*");
    private static final Pattern NUMBERED_HEADING = Pattern.compile("^\\d+(?:\\.\\d+)*[.)]? +\\p{Lu}.*");
    private static final Pattern PAGE_NUMBER = Pattern.compile(
            "(?i)^page +\\d+(?: *(?:of|/) *\\d+)?$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    private static final Set<String> NAV_LABELS = Set.of("home", "docs", "documentation", "about",
            "about us", "contact", "contact us", "login", "log in", "sign in", "sign up", "help",
            "support", "products", "services", "pricing", "blog", "privacy", "terms");
    private static final int EDGE_LINES = 2;
    private static final int PREVIEW_LIMIT = 100;
    private final CleaningOptions options;
    private final Set<String> knownMargins;

    public DefaultTextCleaner() { this(CleaningOptions.defaults()); }
    public DefaultTextCleaner(CleaningOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        Set<String> normalized = new HashSet<>();
        options.knownMarginLines().forEach(s -> {
            String line = normalizeProseLine(s);
            if (!line.isEmpty()) normalized.add(line);
        });
        knownMargins = Set.copyOf(normalized);
    }

    @Override
    public CleanedDocument clean(StructuredDocument document) {
        Objects.requireNonNull(document, "document");
        Audit audit = new Audit();
        List<String> warnings = new ArrayList<>();
        List<Line> lines = toLines(document.blocks(), audit);
        Map<Integer, List<Integer>> pages = pageLines(lines);
        Set<Integer> top = new HashSet<>(), bottom = new HashSet<>();
        pages.values().forEach(indices -> {
            top.addAll(indices.subList(0, Math.min(EDGE_LINES, indices.size())));
            bottom.addAll(indices.subList(Math.max(0, indices.size() - EDGE_LINES), indices.size()));
        });
        Set<String> repeatedTop = Set.of(), repeatedBottom = Set.of();
        if (options.detectRepeatedPageMargins()) {
            warnings.add("Repeated-margin detection is a heuristic. Review its removal preview; "
                    + "repeated body text can be mistaken for a margin.");
            repeatedTop = repeatedMargins(lines, top, document.pageCount());
            repeatedBottom = repeatedMargins(lines, bottom, document.pageCount());
        }
        List<String> output = new ArrayList<>();
        int headings = 0;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            String text = line.text();
            boolean edge = top.contains(i) || bottom.contains(i);
            String reason = null;
            if (!line.protectedContent() && !line.explicitHeading() && !text.isBlank()) {
                if (isNavigationLine(text)) reason = "NAVIGATION_LINE";
                else if (edge && knownMargins.contains(text)) reason = "CONFIGURED_MARGIN";
                else if (edge && line.page() != null && PAGE_NUMBER.matcher(text).matches()) {
                    reason = "PAGE_NUMBER_AT_PAGE_EDGE";
                } else if (!looksLikeHeading(text)
                        && ((top.contains(i) && repeatedTop.contains(text))
                        || (bottom.contains(i) && repeatedBottom.contains(text)))) {
                    reason = "REPEATED_PAGE_MARGIN";
                }
            }
            if (reason != null) {
                audit.remove(reason, line.page(), text);
                continue;
            }
            if (line.explicitHeading()) headings++;
            // At most one empty separator line OUTSIDE protected code/table content.
            if (!line.protectedContent() && text.isBlank()) {
                if (output.isEmpty() || output.getLast().isEmpty()) continue;
                output.add("");
            } else {
                output.add(text);
            }
        }
        while (!output.isEmpty() && output.getLast().isEmpty()) output.removeLast();
        String cleaned = String.join("\n", output);
        if (document.pageCount() == 0) {
            warnings.add("No reliable page boundaries: automatic page-number and repeated-page-margin "
                    + "removal were not applied. Configured exact margins can match document edges.");
        }
        if (document.source().contentType().equals(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            warnings.add("Some DOCX header/footer text is emitted as ordinary paragraphs by Tika. "
                    + "Unmarked text is retained unless an explicit configured edge rule matches.");
        }
        warnings.add("Only supplied heading structure or explicit Markdown heading syntax is recognized. "
                + "Visual-only PDF headings are not reconstructed.");
        warnings.add("Code/table whitespace is protected only when marked. Unmarked code, tables and "
                + "quoted commands in prose require source review.");
        if (cleaned.isBlank()) warnings.add("No usable text remains; do not embed or index this result.");
        var source = document.source();
        return new CleanedDocument(source.fileName(), source.contentType(), source.text(), cleaned,
                source.metadata(), new CleaningReport("day31-v1", source.text().length(),
                cleaned.length(), headings, document.pageCount(), audit.counts, audit.preview,
                audit.total > PREVIEW_LIMIT, warnings));
    }

    private List<Line> toLines(List<TextBlock> blocks, Audit audit) {
        List<Line> result = new ArrayList<>();
        char fenceChar = 0;
        int fenceLength = 0;
        for (TextBlock block : blocks) {
            if (options.removeMarkedBoilerplate() && Set.of(NAVIGATION, HEADER, FOOTER).contains(block.kind())) {
                audit.remove("MARKED_" + block.kind(), block.pageNumber(), block.text());
                continue;
            }
            String normalized = normalizeLineEndings(block.text());
            if (block.kind() == HEADING) {
                String heading = normalizeProseLine(normalized.replace('\n', ' '));
                if (!heading.isBlank()) {
                    result.add(new Line("#".repeat(block.headingLevel()) + " " + heading,
                            block.pageNumber(), false, true));
                }
            } else {
                String[] parts = normalized.split("\n", -1);
                boolean structuredProtected = block.kind() == CODE || block.kind() == TABLE;
                for (int i = 0; i < parts.length; i++) {
                    String raw = parts[i];
                    boolean fenced = fenceChar != 0;
                    var match = FENCE.matcher(raw);
                    if (!structuredProtected && match.matches()) {
                        String marker = match.group(1);
                        if (fenceChar == 0) {
                            fenceChar = marker.charAt(0); fenceLength = marker.length();
                        } else if (marker.charAt(0) == fenceChar && marker.length() >= fenceLength
                                && match.group(2).isBlank()) {
                            fenceChar = 0; fenceLength = 0;
                        }
                        fenced = true;
                    }
                    boolean protectedContent = structuredProtected || fenced || fenceChar != 0
                            || raw.indexOf('`') >= 0;
                    String text = protectedContent ? raw : normalizeProseLine(raw);
                    boolean setext = !text.isBlank() && i + 1 < parts.length
                            && parts[i + 1].strip().matches("(?:={3,}|-{3,})");
                    result.add(new Line(text, block.pageNumber(), protectedContent,
                            !protectedContent && (ATX_HEADING.matcher(text).matches() || setext)));
                }
            }
            // Parser block boundaries are kept as paragraph separators, not collapsed to spaces.
            if (fenceChar == 0) result.add(new Line("", block.pageNumber(), false, false));
        }
        return result;
    }

    private static Map<Integer, List<Integer>> pageLines(List<Line> lines) {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).text().isBlank()) {
                result.computeIfAbsent(lines.get(i).page(), ignored -> new ArrayList<>()).add(i);
            }
        }
        return result;
    }

    private static Set<String> repeatedMargins(List<Line> lines, Set<Integer> edges, int pageCount) {
        if (pageCount < 3) return Set.of();
        Map<String, Set<Integer>> occurrences = new HashMap<>();
        for (int i : edges) {
            Line line = lines.get(i);
            if (line.page() == null || line.protectedContent() || line.explicitHeading()
                    || looksLikeHeading(line.text()) || line.text().length() > 120) continue;
            occurrences.computeIfAbsent(line.text(), ignored -> new HashSet<>()).add(line.page());
        }
        int required = Math.max(3, (int) Math.ceil(pageCount * 0.6));
        Set<String> result = new HashSet<>();
        occurrences.forEach((text, pages) -> { if (pages.size() >= required) result.add(text); });
        return result;
    }

    private static boolean looksLikeHeading(String text) {
        if (text.length() > 100) return false;
        return ATX_HEADING.matcher(text).matches() || NUMBERED_HEADING.matcher(text).matches()
                || (text.codePoints().anyMatch(Character::isLetter)
                && text.equals(text.toUpperCase(Locale.ROOT)));
    }

    private static boolean isNavigationLine(String text) {
        String[] labels = text.split("\\s*[|›»>]\\s*", -1);
        if (labels.length < 3) return false;
        return Arrays.stream(labels).allMatch(label -> NAV_LABELS.contains(label.toLowerCase(Locale.ROOT)));
    }

    public static String normalizeLineEndings(String text) {
        Objects.requireNonNull(text, "text");
        return text.replace("\r\n", "\n").replace('\r', '\n')
                .replace('\u0085', '\n').replace('\u2028', '\n').replace('\u2029', '\n');
    }

    public static String normalizeProseLine(String text) {
        // Do not use \\s+: it would also eat paragraph and page boundaries.
        return HORIZONTAL.matcher(text).replaceAll(" ").strip();
    }

    private record Line(String text, Integer page, boolean protectedContent, boolean explicitHeading) {}
    private static final class Audit {
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private final List<RemovedText> preview = new ArrayList<>();
        private int total;
        void remove(String reason, Integer page, String text) {
            total++;
            counts.merge(reason, 1, Integer::sum);
            if (preview.size() < PREVIEW_LIMIT) {
                String excerpt = text.length() <= 200 ? text : text.substring(0, 200) + "…";
                preview.add(new RemovedText(reason, page, excerpt));
            }
        }
    }
}
