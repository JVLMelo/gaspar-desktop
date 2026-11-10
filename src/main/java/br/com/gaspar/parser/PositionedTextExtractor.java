package br.com.gaspar.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Extrai linhas mantendo as coordenadas necessárias ao fallback do relatório Domínio. */
final class PositionedTextExtractor extends PDFTextStripper {
    private static final float Y_TOLERANCE = 2.0f;
    private static final float WORD_GAP = 1.0f;

    private final List<PageLines> pages = new ArrayList<>();
    private final List<Glyph> currentPage = new ArrayList<>();

    private PositionedTextExtractor() throws IOException {
        setSortByPosition(true);
    }

    static List<PageLines> extract(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PositionedTextExtractor extractor = new PositionedTextExtractor();
            extractor.getText(document);
            return List.copyOf(extractor.pages);
        }
    }

    @Override
    protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
        currentPage.clear();
        super.startPage(page);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        String unicode = text.getUnicode();
        if (unicode != null && !unicode.isEmpty()) {
            currentPage.add(new Glyph(unicode, text.getXDirAdj(), text.getYDirAdj(), text.getWidthDirAdj()));
        }
        super.processTextPosition(text);
    }

    @Override
    protected void endPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
        pages.add(new PageLines(toLines(currentPage)));
        super.endPage(page);
    }

    private static List<TextLine> toLines(List<Glyph> glyphs) {
        List<Glyph> sorted = new ArrayList<>(glyphs);
        sorted.sort(Comparator.comparing(Glyph::y).thenComparing(Glyph::x));
        List<List<Glyph>> grouped = new ArrayList<>();
        for (Glyph glyph : sorted) {
            List<Glyph> line = grouped.stream()
                    .filter(candidate -> Math.abs(candidate.getFirst().y - glyph.y) <= Y_TOLERANCE)
                    .findFirst().orElse(null);
            if (line == null) {
                line = new ArrayList<>();
                grouped.add(line);
            }
            line.add(glyph);
        }
        return grouped.stream().map(line -> {
            line.sort(Comparator.comparing(Glyph::x));
            return new TextLine(buildText(line), List.copyOf(line));
        }).toList();
    }

    private static String buildText(List<Glyph> glyphs) {
        StringBuilder text = new StringBuilder();
        float previousEnd = Float.NaN;
        for (Glyph glyph : glyphs) {
            if (!Float.isNaN(previousEnd) && glyph.x - previousEnd > WORD_GAP) text.append(' ');
            text.append(glyph.text);
            previousEnd = Float.isNaN(previousEnd)
                    ? glyph.x + glyph.width
                    : Math.max(previousEnd, glyph.x + glyph.width);
        }
        return text.toString().strip();
    }

    record PageLines(List<TextLine> lines) {}

    record TextLine(String text, List<Glyph> glyphs) {
        List<Word> words() {
            if (glyphs.isEmpty()) return List.of();
            List<Word> words = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            float start = glyphs.getFirst().x;
            float previousEnd = Float.NaN;
            for (Glyph glyph : glyphs) {
                if (!Float.isNaN(previousEnd) && glyph.x - previousEnd > WORD_GAP) {
                    if (!current.isEmpty()) words.add(new Word(current.toString(), start));
                    current.setLength(0);
                    start = glyph.x;
                }
                current.append(glyph.text);
                previousEnd = Float.isNaN(previousEnd)
                        ? glyph.x + glyph.width
                        : Math.max(previousEnd, glyph.x + glyph.width);
            }
            if (!current.isEmpty()) words.add(new Word(current.toString(), start));
            return words;
        }
    }

    record Glyph(String text, float x, float y, float width) {}
    record Word(String text, float x) {}
}
