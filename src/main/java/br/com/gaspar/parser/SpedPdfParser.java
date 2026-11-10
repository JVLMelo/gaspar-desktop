package br.com.gaspar.parser;

import br.com.gaspar.domain.*;
import br.com.gaspar.exception.ExtracaoIncompletaException;
import br.com.gaspar.exception.ReportParsingException;
import br.com.gaspar.service.IdentificadorRelatorioService;
import br.com.gaspar.util.ValorMonetario;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpedPdfParser implements PdfReportParser<LinhaSped> {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Pattern HEADER_CANDIDATO = Pattern.compile(
            "^\\d{2}/\\d{2}/\\d{4}\\s+(?:\\d{2}/\\d{2}/\\d{4}\\s+)?\\d+");
    private static final Pattern HEADER_SAIDA = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s+(?:(\\d{2}/\\d{2}/\\d{4})\\s+)?(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)$");
    private static final Pattern HEADER_ENTRADA = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+.+$");
    private static final Pattern CFOP_SAIDA = Pattern.compile(
            "^(\\d+)\\s+(\\d+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)$");
    private static final Pattern CFOP_ENTRADA = Pattern.compile(
            "^(\\d+)\\s+(\\d+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)\\s+(-?[\\d.,]+)$");

    private final IdentificadorRelatorioService identificador = new IdentificadorRelatorioService();

    @Override
    public RelatorioExtraido<LinhaSped> parse(Path arquivo) throws ReportParsingException {
        TipoMovimento tipo = identificador.identificar(arquivo).tipo();
        Pattern headerPattern = tipo == TipoMovimento.ENTRADA ? HEADER_ENTRADA : HEADER_SAIDA;
        Pattern cfopPattern = tipo == TipoMovimento.ENTRADA ? CFOP_ENTRADA : CFOP_SAIDA;
        List<LinhaSped> linhas = new ArrayList<>();
        Header atual = null;
        int cabecalhosNaoReconhecidos = 0;

        for (String raw : PdfTextSupport.textoCompleto(arquivo).split("\\R")) {
            String linha = raw.strip();
            Matcher header = headerPattern.matcher(linha);
            if (header.matches()) {
                if (tipo == TipoMovimento.ENTRADA) {
                    atual = new Header(header.group(3), header.group(5), header.group(4), header.group(2));
                } else {
                    atual = new Header(header.group(3), header.group(5), header.group(4), header.group(1));
                }
                continue;
            }
            if (HEADER_CANDIDATO.matcher(linha).find()) {
                cabecalhosNaoReconhecidos++;
                atual = null;
                continue;
            }
            Matcher cfop = cfopPattern.matcher(linha);
            if (cfop.matches() && atual != null) {
                try {
                    linhas.add(new LinhaSped(
                            new ChaveNota(Long.parseLong(atual.nota), Integer.parseInt(atual.serie)),
                            atual.modelo,
                            LocalDate.parse(atual.dataEmissao, DATA),
                            ValorMonetario.parseBrasileiro(cfop.group(4))
                    ));
                } catch (RuntimeException e) {
                    throw new ReportParsingException("O SPED contém uma nota com dados numéricos ou data inválidos.", e);
                }
            }
        }
        if (cabecalhosNaoReconhecidos > 0) {
            throw new ExtracaoIncompletaException(cabecalhosNaoReconhecidos
                    + " cabeçalho(s) de nota do SPED estão em leiaute inesperado. "
                    + "A conferência foi interrompida para não produzir resultado incompleto.");
        }
        return new RelatorioExtraido<>(tipo, linhas);
    }

    private record Header(String nota, String serie, String modelo, String dataEmissao) {}
}
