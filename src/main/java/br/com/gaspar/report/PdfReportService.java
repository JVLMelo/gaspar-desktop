package br.com.gaspar.report;

import br.com.gaspar.domain.NotaComparada;
import br.com.gaspar.domain.ResultadoComparacao;
import br.com.gaspar.domain.ResumoComparacao;
import br.com.gaspar.domain.SituacaoComparacao;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Gera uma versão paginada e legível da conferência em PDF. */
public final class PdfReportService {
    private static final PDRectangle PAGE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGIN = 34;
    private static final float CONTENT_WIDTH = PAGE.getWidth() - (MARGIN * 2);
    private static final float HEADER_HEIGHT = 54;
    private static final float ROW_HEIGHT = 18;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final PDColor BLUE = new PDColor(new float[]{0.09f, 0.25f, 0.39f}, PDDeviceRGB.INSTANCE);
    private static final PDColor LIGHT_BLUE = new PDColor(new float[]{0.94f, 0.97f, 0.99f}, PDDeviceRGB.INSTANCE);
    private static final PDColor BORDER = new PDColor(new float[]{0.78f, 0.83f, 0.88f}, PDDeviceRGB.INSTANCE);
    private static final PDColor TEXT = new PDColor(new float[]{0.10f, 0.14f, 0.20f}, PDDeviceRGB.INSTANCE);
    private static final PDColor MUTED = new PDColor(new float[]{0.32f, 0.40f, 0.48f}, PDDeviceRGB.INSTANCE);
    private static final PDColor WHITE = new PDColor(new float[]{1, 1, 1}, PDDeviceRGB.INSTANCE);

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public void gerar(ResultadoComparacao resultado, Path destino) throws IOException {
        Path absolute = destino.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".gaspar-", ".pdf");
        try (PDDocument document = new PDDocument()) {
            render(document, resultado);
            document.save(temporary.toFile());
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void render(PDDocument document, ResultadoComparacao resultado) throws IOException {
        Page page = newPage(document, resultado);
        float y = page.topContentY();
        y = drawTitle(page.stream(), resultado, y);
        y -= 18;
        y = drawSummary(page.stream(), resultado.resumo(), y);
        y -= 22;
        y = drawNotesHeader(page.stream(), y);
        for (NotaComparada nota : resultado.notas()) {
            if (y < MARGIN + 30) {
                finishPage(page);
                page = newPage(document, resultado);
                y = page.topContentY();
                y = drawNotesHeader(page.stream(), y);
            }
            drawNoteRow(page.stream(), nota, y);
            y -= ROW_HEIGHT;
        }
        finishPage(page);
    }

    private Page newPage(PDDocument document, ResultadoComparacao resultado) throws IOException {
        PDPage page = new PDPage(PAGE);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);
        stream.setNonStrokingColor(BLUE);
        stream.addRect(0, PAGE.getHeight() - HEADER_HEIGHT, PAGE.getWidth(), HEADER_HEIGHT);
        stream.fill();
        write(stream, bold, 16,  MARGIN, PAGE.getHeight() - 23, "Gaspar - Conferencia Dominio x SPED", WHITE);
        write(stream, regular, 9, MARGIN, PAGE.getHeight() - 39,
                "Relatorio de " + resultado.tipoMovimento().descricao() + " | Gerado em " + DATE_TIME.format(LocalDateTime.now()),
                new PDColor(new float[]{0.86f, 0.92f, 0.97f}, PDDeviceRGB.INSTANCE));
        return new Page(page, stream, PAGE.getHeight() - HEADER_HEIGHT - 20, document.getNumberOfPages());
    }

    private float drawTitle(PDPageContentStream stream, ResultadoComparacao resultado, float y) throws IOException {
        write(stream, bold, 13, MARGIN, y, "Resumo da conferencia", TEXT);
        write(stream, regular, 9, MARGIN, y - 14,
                resultado.arquivosTrocados() ? "A ordem dos arquivos foi identificada e ajustada automaticamente." :
                        "Os dois relatorios foram processados localmente.", MUTED);
        return y - 30;
    }

    private float drawSummary(PDPageContentStream stream, ResumoComparacao resumo, float y) throws IOException {
        String[][] values = {
                {"Notas no Dominio", Long.toString(resumo.notasDominio())},
                {"Notas no SPED", Long.toString(resumo.notasSped())},
                {"Iguais (OK)", Long.toString(resumo.iguais())},
                {"Apenas no Dominio", Long.toString(resumo.apenasDominio())},
                {"Apenas no SPED", Long.toString(resumo.apenasSped())},
                {"Divergencias", Long.toString(resumo.divergencias())},
                {"Total Dominio", money(resumo.valorTotalDominio())},
                {"Total SPED", money(resumo.valorTotalSped())},
                {"Diferenca", money(resumo.diferenca())}
        };
        float gap = 7;
        float width = (CONTENT_WIDTH - (gap * 4)) / 5;
        float height = 41;
        for (int i = 0; i < values.length; i++) {
            int row = i / 5;
            int col = i % 5;
            float x = MARGIN + col * (width + gap);
            float top = y - row * (height + gap);
            stream.setNonStrokingColor(LIGHT_BLUE);
            stream.addRect(x, top - height, width, height);
            stream.fill();
            stream.setStrokingColor(BORDER);
            stream.addRect(x, top - height, width, height);
            stream.stroke();
            write(stream, regular, 8, x + 7, top - 14, values[i][0], MUTED);
            write(stream, bold, 13, x + 7, top - 31, values[i][1], TEXT);
        }
        return y - (height * 2 + gap) - 3;
    }

    private float drawNotesHeader(PDPageContentStream stream, float y) throws IOException {
        write(stream, bold, 12, MARGIN, y, "Notas conferidas", TEXT);
        float headerTop = y - 12;
        float[] widths = columnWidths();
        stream.setNonStrokingColor(BLUE);
        stream.addRect(MARGIN, headerTop - ROW_HEIGHT, CONTENT_WIDTH, ROW_HEIGHT);
        stream.fill();
        String[] labels = {"Nota", "Serie", "Data Dominio", "Especie", "Valor Dominio", "Data SPED", "Modelo", "Valor SPED", "Situacao"};
        float x = MARGIN;
        for (int i = 0; i < labels.length; i++) {
            write(stream, bold, 7.3f, x + 3, headerTop - 12, labels[i], WHITE);
            x += widths[i];
        }
        return headerTop - ROW_HEIGHT;
    }

    private void drawNoteRow(PDPageContentStream stream, NotaComparada nota, float y) throws IOException {
        float[] widths = columnWidths();
        if (((int) ((PAGE.getHeight() - y) / ROW_HEIGHT)) % 2 == 0) {
            stream.setNonStrokingColor(new PDColor(new float[]{0.985f, 0.99f, 0.995f}, PDDeviceRGB.INSTANCE));
            stream.addRect(MARGIN, y - ROW_HEIGHT, CONTENT_WIDTH, ROW_HEIGHT);
            stream.fill();
        }
        stream.setStrokingColor(BORDER);
        stream.moveTo(MARGIN, y - ROW_HEIGHT);
        stream.lineTo(MARGIN + CONTENT_WIDTH, y - ROW_HEIGHT);
        stream.stroke();
        String[] values = {
                Long.toString(nota.chave().nota()), Integer.toString(nota.chave().serie()), date(nota.dataDominio()),
                text(nota.especieDominio()), money(nota.valorDominio()), date(nota.dataSped()), text(nota.modeloSped()),
                money(nota.valorSped()), nota.situacao().descricao()
        };
        float x = MARGIN;
        for (int i = 0; i < values.length; i++) {
            String value = truncate(values[i], widths[i] - 6, 7.2f);
            write(stream, i == 8 ? bold : regular, 7.2f, x + 3, y - 12, value, situationColor(nota.situacao(), i == 8));
            x += widths[i];
        }
    }

    private void finishPage(Page page) throws IOException {
        write(page.stream(), regular, 8, MARGIN, 17, "Gaspar - Conferencia fiscal", MUTED);
        write(page.stream(), regular, 8, PAGE.getWidth() - MARGIN - 42, 17, "Pagina " + page.number(), MUTED);
        page.stream().close();
    }

    private float[] columnWidths() {
        return new float[]{52, 36, 66, 48, 78, 66, 48, 78, CONTENT_WIDTH - (52 + 36 + 66 + 48 + 78 + 66 + 48 + 78)};
    }

    private PDColor situationColor(SituacaoComparacao situation, boolean status) {
        if (!status) return TEXT;
        return switch (situation) {
            case OK -> new PDColor(new float[]{0.12f, 0.45f, 0.28f}, PDDeviceRGB.INSTANCE);
            case DIVERGENCIA_VALOR -> new PDColor(new float[]{0.55f, 0.35f, 0.04f}, PDDeviceRGB.INSTANCE);
            case APENAS_DOMINIO, APENAS_SPED -> new PDColor(new float[]{0.62f, 0.16f, 0.18f}, PDDeviceRGB.INSTANCE);
        };
    }

    private void write(PDPageContentStream stream, PDType1Font font, float size, float x, float y,
                       String value, PDColor color) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, y);
        stream.showText(safe(value));
        stream.endText();
    }

    private String truncate(String value, float width, float fontSize) throws IOException {
        String safe = safe(value);
        if (regular.getStringWidth(safe) / 1000 * fontSize <= width) return safe;
        String suffix = "...";
        String result = safe;
        while (!result.isEmpty() && regular.getStringWidth(result + suffix) / 1000 * fontSize > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replace('\u2014', '-').replace('\u2013', '-').replace('\u00d7', 'x');
    }

    private String date(LocalDate value) { return value == null ? "-" : DATE.format(value); }
    private String money(BigDecimal value) { return value == null ? "-" : MONEY.format(value); }
    private String text(String value) { return value == null || value.isBlank() ? "-" : value; }

    private record Page(PDPage page, PDPageContentStream stream, float topContentY, int number) {}
}
