package br.com.gaspar.report;

import br.com.gaspar.domain.ChaveNota;
import br.com.gaspar.domain.NotaComparada;
import br.com.gaspar.domain.ResumoComparacao;
import br.com.gaspar.domain.ResultadoComparacao;
import br.com.gaspar.domain.SituacaoComparacao;
import br.com.gaspar.domain.TipoMovimento;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReportServiceTest {
    @TempDir Path temp;

    @Test
    void geraPdfComResumoENotas() throws Exception {
        NotaComparada ok = new NotaComparada(new ChaveNota(123, 1), LocalDate.of(2026, 1, 2),
                "55", new BigDecimal("10.00"), LocalDate.of(2026, 1, 2), "55",
                new BigDecimal("10.00"), SituacaoComparacao.OK);
        NotaComparada missing = new NotaComparada(new ChaveNota(124, 1), LocalDate.of(2026, 1, 2),
                "55", new BigDecimal("7.00"), null, null, null, SituacaoComparacao.APENAS_DOMINIO);
        ResultadoComparacao result = new ResultadoComparacao(TipoMovimento.SAIDA, List.of(ok, missing),
                new ResumoComparacao(2, 1, 1, 1, 0, 0, new BigDecimal("17.00"),
                        new BigDecimal("10.00"), new BigDecimal("7.00")), false);
        Path output = temp.resolve("resultado.pdf");

        new PdfReportService().gerar(result, output);

        assertTrue(java.nio.file.Files.isRegularFile(output));
        try (var document = Loader.loadPDF(output.toFile())) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Gaspar - Conferencia Dominio x SPED"));
            assertTrue(text.contains("Notas no Dominio"));
            assertTrue(text.contains("123"));
            assertTrue(text.contains("Apenas no Dominio"));
        }
    }
}
