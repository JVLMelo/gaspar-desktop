package br.com.gaspar.parser;

import br.com.gaspar.domain.*;
import br.com.gaspar.exception.ExtracaoIncompletaException;
import br.com.gaspar.exception.ReportParsingException;
import br.com.gaspar.service.IdentificadorRelatorioService;
import br.com.gaspar.util.ValorMonetario;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DominioPdfParser implements PdfReportParser<LinhaDominio> {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Pattern CANDIDATA = Pattern.compile("\\d{2}/\\d{2}/\\d{4}.*-?\\d,\\d{2}");
    private static final Pattern PREFIXO = Pattern.compile(
            "^\\s*(?:\\d+\\s+)?(\\d+)\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+");
    private static final Pattern VALOR_VALIDO = Pattern.compile("^-?[\\d.]+,\\d{2}$");
    private static final Pattern LINHA_SAIDA = Pattern.compile(
            "^\\s*(?:\\d+\\s+)?(\\d+)\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.*?)\\s+"
                    + "(\\d-\\d{3})\\s+\\d+\\s+([A-Z]{2})\\s*(-?[\\d.,]+)\\s*ICMS");
    private static final Pattern LINHA_ENTRADA = Pattern.compile(
            "^\\s*(?:\\d+\\s+)?(\\d+)\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+.+?\\s*"
                    + "(-?[\\d.,]+)\\s*ICMS");

    private final IdentificadorRelatorioService identificador = new IdentificadorRelatorioService();

    @Override
    public RelatorioExtraido<LinhaDominio> parse(Path arquivo) throws ReportParsingException {
        PdfTextSupport.validarPdf(arquivo);
        TipoMovimento tipo = identificador.identificar(arquivo).tipo();
        Pattern linhaPattern = tipo == TipoMovimento.ENTRADA ? LINHA_ENTRADA : LINHA_SAIDA;
        List<LinhaDominio> linhas = new ArrayList<>();
        int naoReconhecidas = 0;
        ColumnBounds limites = null;

        final List<PositionedTextExtractor.PageLines> pages;
        try {
            pages = PositionedTextExtractor.extract(arquivo);
        } catch (IOException e) {
            throw new ReportParsingException("Não foi possível analisar as posições do PDF do Domínio.", e);
        }

        for (PositionedTextExtractor.PageLines page : pages) {
            if (limites == null) limites = localizarColunaValor(page);
            for (PositionedTextExtractor.TextLine textLine : page.lines()) {
                String linha = textLine.text().strip();
                Matcher matcher = linhaPattern.matcher(linha);
                String data;
                String nota;
                String serie;
                String especie;
                String valor;
                if (matcher.find()) {
                    data = matcher.group(2);
                    nota = matcher.group(3);
                    serie = matcher.group(4);
                    especie = matcher.group(5);
                    valor = tipo == TipoMovimento.ENTRADA ? matcher.group(7) : matcher.group(10);
                } else {
                    if (!CANDIDATA.matcher(linha).find() || linha.toLowerCase().contains("total")) continue;
                    Matcher prefixo = PREFIXO.matcher(linha);
                    valor = limites == null ? null : valorPorPosicao(textLine, limites);
                    if (!prefixo.find() || valor == null) {
                        naoReconhecidas++;
                        continue;
                    }
                    data = prefixo.group(2);
                    nota = prefixo.group(3);
                    serie = prefixo.group(4);
                    especie = prefixo.group(5);
                }
                try {
                    linhas.add(new LinhaDominio(
                            new ChaveNota(Long.parseLong(nota), Integer.parseInt(serie)),
                            especie,
                            LocalDate.parse(data, DATA),
                            ValorMonetario.parseBrasileiro(valor)
                    ));
                } catch (RuntimeException e) {
                    throw new ReportParsingException("O relatório Domínio contém uma nota com dados numéricos ou data inválidos.", e);
                }
            }
        }

        if (naoReconhecidas > 0) {
            throw new ExtracaoIncompletaException(naoReconhecidas
                    + " linha(s) do relatório Domínio parecem notas, mas não foram reconhecidas. "
                    + "A conferência foi interrompida para não produzir resultado incompleto.");
        }
        return new RelatorioExtraido<>(tipo, linhas);
    }

    private ColumnBounds localizarColunaValor(PositionedTextExtractor.PageLines page) {
        for (PositionedTextExtractor.TextLine line : page.lines()) {
            Float xValor = null;
            for (PositionedTextExtractor.Word word : line.words()) {
                if (xValor == null && word.text().startsWith("Valor")) {
                    xValor = word.x();
                } else if (xValor != null && word.text().equals("Tipo")) {
                    return new ColumnBounds(xValor, word.x());
                }
            }
        }
        return null;
    }

    private String valorPorPosicao(PositionedTextExtractor.TextLine line, ColumnBounds bounds) {
        StringBuilder valor = new StringBuilder();
        for (PositionedTextExtractor.Glyph glyph : line.glyphs()) {
            if (glyph.x() < bounds.xValor || glyph.x() >= bounds.xTipo) continue;
            for (char character : glyph.text().toCharArray()) {
                if ("0123456789.,-".indexOf(character) >= 0) valor.append(character);
            }
        }
        String resultado = valor.toString();
        return VALOR_VALIDO.matcher(resultado).matches() ? resultado : null;
    }

    private record ColumnBounds(float xValor, float xTipo) {}
}
