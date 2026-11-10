package br.com.gaspar.service;

import br.com.gaspar.domain.*;
import br.com.gaspar.exception.ReportParsingException;
import br.com.gaspar.parser.DominioPdfParser;
import br.com.gaspar.parser.SpedPdfParser;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class ConferenciaService {
    private final IdentificadorRelatorioService identificador = new IdentificadorRelatorioService();
    private final DominioPdfParser dominioParser = new DominioPdfParser();
    private final SpedPdfParser spedParser = new SpedPdfParser();
    private final ComparacaoService comparacaoService = new ComparacaoService();

    public ResultadoComparacao conferir(Path selecionadoDominio, Path selecionadoSped,
                                        Consumer<String> progresso) throws ReportParsingException {
        Consumer<String> status = progresso == null ? ignored -> {} : progresso;
        status.accept("Validando os relatórios…");
        var idDominio = identificador.identificar(selecionadoDominio);
        var idSped = identificador.identificar(selecionadoSped);

        Path dominio = selecionadoDominio;
        Path sped = selecionadoSped;
        boolean trocados = false;
        if (idDominio.origem() == OrigemRelatorio.SPED && idSped.origem() == OrigemRelatorio.DOMINIO) {
            dominio = selecionadoSped;
            sped = selecionadoDominio;
            var temporario = idDominio;
            idDominio = idSped;
            idSped = temporario;
            trocados = true;
        }
        if (idDominio.origem() != OrigemRelatorio.DOMINIO) {
            throw new ReportParsingException("O arquivo informado como Domínio não foi reconhecido como relatório Acompanhamento de Entradas/Saídas.");
        }
        if (idSped.origem() != OrigemRelatorio.SPED) {
            throw new ReportParsingException("O arquivo informado como SPED não foi reconhecido como relatório de Registros Fiscais.");
        }
        if (idDominio.tipo() != idSped.tipo()) {
            throw new ReportParsingException("Os relatórios são de tipos diferentes. Envie ambos de Entrada ou ambos de Saída.");
        }

        status.accept("Lendo relatório do Domínio…");
        RelatorioExtraido<LinhaDominio> relatorioDominio = dominioParser.parse(dominio);
        status.accept("Lendo relatório do SPED…");
        RelatorioExtraido<LinhaSped> relatorioSped = spedParser.parse(sped);
        if (relatorioDominio.tipo() != relatorioSped.tipo()) {
            throw new ReportParsingException("Os leitores identificaram tipos de movimento incompatíveis.");
        }
        if (relatorioDominio.linhas().isEmpty() || relatorioSped.linhas().isEmpty()) {
            throw new ReportParsingException("Não foi possível extrair notas de um dos PDFs. Confira se os relatórios são os corretos.");
        }
        status.accept("Comparando as notas…");
        return comparacaoService.comparar(relatorioDominio.tipo(), relatorioSped.linhas(),
                relatorioDominio.linhas(), trocados);
    }
}
