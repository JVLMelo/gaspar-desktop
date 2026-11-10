package br.com.gaspar.report;

import br.com.gaspar.domain.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReportServiceTest {
    @TempDir Path temp;

    @Test
    void geraCincoAbasFormatadas() throws Exception {
        NotaComparada ok = new NotaComparada(new ChaveNota(123, 1), LocalDate.of(2026, 1, 2),
                "55", new BigDecimal("10.00"), LocalDate.of(2026, 1, 2), "55",
                new BigDecimal("10.00"), SituacaoComparacao.OK);
        NotaComparada missing = new NotaComparada(new ChaveNota(124, 1), LocalDate.of(2026, 1, 2),
                "55", new BigDecimal("7.00"), null, null, null, SituacaoComparacao.APENAS_DOMINIO);
        ResultadoComparacao result = new ResultadoComparacao(TipoMovimento.SAIDA, List.of(ok, missing),
                new ResumoComparacao(2, 1, 1, 1, 0, 0, new BigDecimal("17.00"),
                        new BigDecimal("10.00"), new BigDecimal("7.00")), false);
        Path output = temp.resolve("resultado.xlsx");

        new ExcelReportService().gerar(result, output);

        assertTrue(Files.isRegularFile(output));
        try (var input = Files.newInputStream(output); var workbook = new XSSFWorkbook(input)) {
            assertEquals(List.of("Resumo", "Apenas no Domínio", "Apenas no SPED", "Divergências", "Todas as Notas"),
                    java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
                            .mapToObj(workbook::getSheetName).toList());
            assertEquals(3, workbook.getSheet("Todas as Notas").getPhysicalNumberOfRows());
            assertEquals("situacao", workbook.getSheet("Todas as Notas").getRow(0).getCell(8).getStringCellValue());
            assertEquals("Apenas no Domínio", workbook.getSheet("Todas as Notas").getRow(2).getCell(8).getStringCellValue());
            assertTrue(workbook.getSheet("Todas as Notas").getCTWorksheet().isSetAutoFilter());
        }
    }
}
