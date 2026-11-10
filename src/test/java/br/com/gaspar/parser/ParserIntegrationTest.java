package br.com.gaspar.parser;

import br.com.gaspar.domain.SituacaoComparacao;
import br.com.gaspar.domain.TipoMovimento;
import br.com.gaspar.service.ConferenciaService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserIntegrationTest {
    @TempDir Path temp;

    @Test
    void executaFluxoCompletoETrataNfceSemSegundaData() throws Exception {
        Path dominio = pdf("dominio.pdf", List.of(
                "ACOMPANHAMENTO DE SAIDAS",
                "Codigo Data Nota Serie Especie Cliente Nome CFOP Cod UF Valor Tipo",
                "1 389 09/07/2026 114 2 65 1 CLIENTES DIVERSOS 5-405 572 PB 15,00 ICMS 0,00"
        ));
        Path sped = pdf("sped.pdf", List.of(
                "REGISTROS FISCAIS SAIDAS DE MERCADORIAS",
                "09/07/2026 114 65 002 00",
                "000 5405 0,00 15,00 0,00 0,00 0,00 0,00 0,00"
        ));

        var result = new ConferenciaService().conferir(dominio, sped, ignored -> {});

        assertEquals(TipoMovimento.SAIDA, result.tipoMovimento());
        assertEquals(1, result.notas().size());
        assertEquals(SituacaoComparacao.OK, result.notas().getFirst().situacao());
        assertEquals("15.00", result.notas().getFirst().valorDominio().toPlainString());
        assertEquals("15.00", result.notas().getFirst().valorSped().toPlainString());
    }

    @Test
    void corrigeArquivosInvertidos() throws Exception {
        Path dominio = pdf("dominio-invertido.pdf", List.of(
                "ACOMPANHAMENTO DE SAIDAS",
                "Codigo Data Nota Serie Especie Cliente Nome CFOP Cod UF Valor Tipo",
                "1 389 09/07/2026 114 2 65 1 CLIENTES DIVERSOS 5-405 572 PB 15,00 ICMS 0,00"
        ));
        Path sped = pdf("sped-invertido.pdf", List.of(
                "REGISTROS FISCAIS SAIDAS DE MERCADORIAS",
                "09/07/2026 09/07/2026 114 65 002 00",
                "000 5405 0,00 15,00 0,00 0,00 0,00 0,00 0,00"
        ));

        String extracted = PdfTextSupport.textoCompleto(sped);
        assertTrue(extracted.contains("09/07/2026 09/07/2026 114 65 002 00"), extracted);
        assertTrue(extracted.contains("000 5405 0,00 15,00 0,00 0,00 0,00 0,00 0,00"), extracted);
        var parsed = new SpedPdfParser().parse(sped);
        assertEquals(TipoMovimento.SAIDA, parsed.tipo());
        assertEquals(1, parsed.linhas().size(), extracted);
        var result = new ConferenciaService().conferir(sped, dominio, ignored -> {});
        assertTrue(result.arquivosTrocados());
        assertEquals(SituacaoComparacao.OK, result.notas().getFirst().situacao());
    }

    @Test
    void recuperaValorDominioPelaPosicaoQuandoTextoEstaSobreposto() throws Exception {
        Path output = temp.resolve("dominio-sobreposto.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 7);
                textAt(content, 30, 750, "ACOMPANHAMENTO DE ENTRADAS");
                textAt(content, 400, 720, "Valor");
                textAt(content, 460, 720, "Tipo");
                textAt(content, 30, 690,
                        "1 389 09/07/2026 135393 1 41 999 FORNECEDOR MUITO COMPRIDO");
                textAt(content, 400, 690, "693,98");
                textAt(content, 450, 690, "X");
                textAt(content, 470, 690, "ICMS");
                content.endText();
            }
            document.save(output.toFile());
        }

        var positioned = PositionedTextExtractor.extract(output);
        String debug = positioned.getFirst().lines().stream()
                .map(line -> line.text() + " | " + line.words())
                .collect(java.util.stream.Collectors.joining("\n"));
        assertTrue(debug.contains("Valor"), debug);
        assertTrue(debug.contains("Tipo"), debug);
        String window = positioned.getFirst().lines().stream()
                .filter(line -> line.text().contains("135393"))
                .flatMap(line -> line.glyphs().stream())
                .filter(glyph -> glyph.x() >= 400 && glyph.x() < 460)
                .map(PositionedTextExtractor.Glyph::text)
                .collect(java.util.stream.Collectors.joining());
        assertTrue(window.contains("693,98"), debug + "\nwindow=" + window);
        String dataLine = positioned.getFirst().lines().stream()
                .map(PositionedTextExtractor.TextLine::text)
                .filter(line -> line.contains("135393"))
                .findFirst().orElseThrow();
        assertTrue(java.util.regex.Pattern.compile(
                "^\\s*(?:\\d+\\s+)?(\\d+)\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+")
                .matcher(dataLine).find(), debug + "\nwindow=" + window);
        var headerWords = positioned.getFirst().lines().stream()
                .filter(line -> line.text().contains("Valor") && line.text().contains("Tipo"))
                .findFirst().orElseThrow().words();
        float xValor = headerWords.stream().filter(word -> word.text().equals("Valor"))
                .findFirst().orElseThrow().x();
        float xTipo = headerWords.stream().filter(word -> word.text().equals("Tipo"))
                .findFirst().orElseThrow().x();
        String recovered = positioned.getFirst().lines().stream()
                .filter(line -> line.text().contains("135393"))
                .flatMap(line -> line.glyphs().stream())
                .filter(glyph -> glyph.x() >= xValor && glyph.x() < xTipo)
                .flatMapToInt(glyph -> glyph.text().chars())
                .filter(character -> "0123456789.,-".indexOf(character) >= 0)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        assertEquals("693,98", recovered, debug);
        var parsed = new DominioPdfParser().parse(output);
        assertEquals(TipoMovimento.ENTRADA, parsed.tipo());
        assertEquals(1, parsed.linhas().size());
        assertEquals(135393, parsed.linhas().getFirst().chave().nota());
        assertEquals("693.98", parsed.linhas().getFirst().valorContabil().toPlainString());
    }

    private Path pdf(String name, List<String> lines) throws Exception {
        Path output = temp.resolve(name);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 7);
                content.newLineAtOffset(30, 750);
                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -18);
                }
                content.endText();
            }
            document.save(output.toFile());
        }
        return output;
    }

    private void textAt(PDPageContentStream content, float x, float y, String text) throws Exception {
        content.setTextMatrix(Matrix.getTranslateInstance(x, y));
        content.showText(text);
    }
}
