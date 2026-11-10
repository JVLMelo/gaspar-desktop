package br.com.gaspar.report;

import br.com.gaspar.domain.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

public final class ExcelReportService {
    private static final String[] COLUNAS = {
            "nota", "serie", "data_dominio", "especie_dominio", "valor_dominio",
            "data_sped", "modelo_sped", "valor_sped", "situacao"
    };

    public void gerar(ResultadoComparacao resultado, Path destino) throws IOException {
        Path absolute = destino.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporario = Files.createTempFile(parent, ".gaspar-", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            criarResumo(workbook, resultado.resumo(), styles);
            criarNotas(workbook, "Apenas no Domínio", resultado.notas(),
                    n -> n.situacao() == SituacaoComparacao.APENAS_DOMINIO, styles);
            criarNotas(workbook, "Apenas no SPED", resultado.notas(),
                    n -> n.situacao() == SituacaoComparacao.APENAS_SPED, styles);
            criarNotas(workbook, "Divergências", resultado.notas(),
                    n -> n.situacao() == SituacaoComparacao.DIVERGENCIA_VALOR, styles);
            criarNotas(workbook, "Todas as Notas", resultado.notas(), n -> true, styles);
            try (OutputStream output = Files.newOutputStream(temporario, StandardOpenOption.TRUNCATE_EXISTING)) {
                workbook.write(output);
            }
            try {
                Files.move(temporario, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private void criarResumo(Workbook workbook, ResumoComparacao resumo, Styles styles) {
        Sheet sheet = workbook.createSheet("Resumo");
        Row header = sheet.createRow(0);
        cell(header, 0, "Informação", styles.header);
        cell(header, 1, "Quantidade", styles.header);
        Object[][] valores = {
                {"Notas no Domínio", resumo.notasDominio()},
                {"Notas no SPED", resumo.notasSped()},
                {"Iguais (OK)", resumo.iguais()},
                {"Apenas no Domínio", resumo.apenasDominio()},
                {"Apenas no SPED", resumo.apenasSped()},
                {"Divergências (valor)", resumo.divergencias()},
                {"Valor total Domínio (R$)", resumo.valorTotalDominio()},
                {"Valor total SPED (R$)", resumo.valorTotalSped()},
                {"Diferença (R$)", resumo.diferenca()}
        };
        for (int i = 0; i < valores.length; i++) {
            Row row = sheet.createRow(i + 1);
            cell(row, 0, valores[i][0].toString(), null);
            if (valores[i][1] instanceof BigDecimal decimal) {
                numeric(row, 1, decimal, styles.money);
            } else {
                numeric(row, 1, ((Number) valores[i][1]).longValue(), styles.integer);
            }
        }
        finalizar(sheet, 2, valores.length + 1);
    }

    private void criarNotas(Workbook workbook, String nome, List<NotaComparada> todas,
                            Predicate<NotaComparada> filtro, Styles styles) {
        Sheet sheet = workbook.createSheet(nome);
        Row header = sheet.createRow(0);
        for (int i = 0; i < COLUNAS.length; i++) cell(header, i, COLUNAS[i], styles.header);
        int rowIndex = 1;
        for (NotaComparada nota : todas) {
            if (!filtro.test(nota)) continue;
            Row row = sheet.createRow(rowIndex++);
            numeric(row, 0, nota.chave().nota(), styles.integer);
            numeric(row, 1, nota.chave().serie(), styles.integer);
            date(row, 2, nota.dataDominio(), styles.date);
            cell(row, 3, nota.especieDominio(), null);
            money(row, 4, nota.valorDominio(), styles.money);
            date(row, 5, nota.dataSped(), styles.date);
            cell(row, 6, nota.modeloSped(), null);
            money(row, 7, nota.valorSped(), styles.money);
            Cell situacao = cell(row, 8, nota.situacao().descricao(), styles.forSituation(nota.situacao()));
            situacao.setCellStyle(styles.forSituation(nota.situacao()));
        }
        finalizar(sheet, COLUNAS.length, rowIndex);
    }

    private void finalizar(Sheet sheet, int columns, int rows) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rows - 1), 0, columns - 1));
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 40 * 256));
        }
    }

    private Cell cell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
        return cell;
    }

    private void numeric(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    private void numeric(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        if (style != null) cell.setCellStyle(style);
    }

    private void money(Row row, int column, BigDecimal value, CellStyle style) {
        if (value != null) numeric(row, column, value, style);
    }

    private void date(Row row, int column, LocalDate value, CellStyle style) {
        if (value == null) return;
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static final class Styles {
        private final CellStyle header;
        private final CellStyle date;
        private final CellStyle money;
        private final CellStyle integer;
        private final CellStyle ok;
        private final CellStyle warning;
        private final CellStyle missing;

        private Styles(Workbook workbook) {
            header = workbook.createCellStyle();
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            header.setFont(font);

            DataFormat format = workbook.createDataFormat();
            date = workbook.createCellStyle();
            date.setDataFormat(format.getFormat("dd/mm/yyyy"));
            money = workbook.createCellStyle();
            money.setDataFormat(format.getFormat("R$ #,##0.00;[Red]-R$ #,##0.00"));
            integer = workbook.createCellStyle();
            integer.setDataFormat(format.getFormat("0"));
            ok = fill(workbook, IndexedColors.LIGHT_GREEN);
            warning = fill(workbook, IndexedColors.LIGHT_YELLOW);
            missing = fill(workbook, IndexedColors.ROSE);
        }

        private CellStyle fill(Workbook workbook, IndexedColors color) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(color.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }

        private CellStyle forSituation(SituacaoComparacao situacao) {
            return switch (situacao) {
                case OK -> ok;
                case DIVERGENCIA_VALOR -> warning;
                case APENAS_DOMINIO, APENAS_SPED -> missing;
            };
        }
    }
}
