package com.javed.java_pgvector.ingestion.parser;

import com.javed.java_pgvector.dto.ParsedDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.javed.java_pgvector.ingestion.parser.DocumentParsingException.Reason.*;

@Component
public class TikaDocumentParser implements DocumentParser{

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/html", "application/xhtml+xml"
    );

    private final int maxInputBytes;
    private final int maxCharacters;

    public TikaDocumentParser(
            @Value("${rag.parser.max-input-bytes:10485760}") int maxInputBytes,
            @Value("${rag.parser.max-characters:1000000}") int maxCharacters) {
        if (maxInputBytes < 1 || maxInputBytes == Integer.MAX_VALUE
                || maxCharacters < 1) {
            throw new IllegalArgumentException("Parser limits must be positive and bounded");
        }
        this.maxInputBytes = maxInputBytes;
        this.maxCharacters = maxCharacters;
    }

    @Override
    public ParsedDocument parse(InputStream input, String fileName) throws IOException {

        Objects.requireNonNull(input, "input");

        //Bounded buffering for this small-file lesson, not an unbounded readAllBytes().
        byte[] bytes = input.readNBytes(maxInputBytes + 1);
        if (bytes.length == 0) {
            throw new DocumentParsingException(EMPTY_FILE, "The file is empty.");
        }
        if (bytes.length > maxInputBytes) {
            throw new DocumentParsingException(INPUT_TOO_LARGE,
                    "The file exceeds the configured input-size limit.");
        }

        String name = baseName(fileName);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, name);
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(maxCharacters);
        ParseContext context = newContext();

        try (TikaInputStream stream = TikaInputStream.get(bytes, metadata)) {
            String contentType = parser.getDetector().detect(stream, metadata).getBaseType().toString();
            if (!ALLOWED_TYPES.contains(contentType)) {
                throw new DocumentParsingException(UNSUPPORTED_TYPE,
                        "Only PDF, DOCX, TXT and HTML are supported.");
            }

            parser.parse(stream, handler, metadata, context);
            String text = handler.toString();
            if (text.isBlank()) {
                throw new DocumentParsingException(NO_TEXT,
                        "No extractable text was found. A scanned PDF may need OCR.");
            }

            Map<String, List<String>> values = new LinkedHashMap<>();
            for (String key : metadata.names()) {
                values.put(key, List.of(metadata.getValues(key)));
            }
            return new ParsedDocument(name, contentType, text, values);

        }catch (TikaException | SAXException | IOException ex) {
            if (WriteLimitReachedException.isWriteLimitReached(ex)) {
                throw new DocumentParsingException(TEXT_TOO_LARGE,
                        "Extracted text exceeds the configured character limit.", ex);
            }
            throw new DocumentParsingException(PARSE_FAILED,
                    "The document could not be parsed. It may be damaged or encrypted.", ex);
        }
    }

    private static ParseContext newContext() {
        ParseContext context = new ParseContext();

        PDFParserConfig pdf = new PDFParserConfig();
        pdf.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        pdf.setExtractInlineImages(false);
        pdf.setCatchIntermediateIOExceptions(false);
        context.set(PDFParserConfig.class, pdf);

        TesseractOCRConfig ocr = new TesseractOCRConfig();
        ocr.setSkipOcr(true);
        context.set(TesseractOCRConfig.class, ocr);

        // Keep embedded attachments separate from the parent document's evidence.
        context.set(EmbeddedDocumentExtractor.class, new EmbeddedDocumentExtractor() {
            @Override
            public boolean shouldParseEmbedded(Metadata metadata) {
                return false;
            }

            @Override
            public void parseEmbedded(InputStream stream, ContentHandler handler,
                                      Metadata metadata, boolean outputHtml) {
                // Embedded attachments are deliberately excluded in Day 30.
            }
        });
        return context;

    }

    private static String baseName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "upload";
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.isBlank() ? "upload" : name;
    }
}
