package br.com.gaspar.service;

import br.com.gaspar.domain.OrigemRelatorio;
import br.com.gaspar.domain.TipoMovimento;
import br.com.gaspar.exception.ReportParsingException;
import br.com.gaspar.parser.PdfTextSupport;

import java.nio.file.Path;
import java.util.Locale;

public final class IdentificadorRelatorioService {
    public Identificacao identificar(Path arquivo) throws ReportParsingException {
        String texto = normalizar(PdfTextSupport.primeiraPagina(arquivo));
        OrigemRelatorio origem;
        if (texto.contains("ACOMPANHAMENTO DE SAIDAS") || texto.contains("ACOMPANHAMENTO DE ENTRADAS")) {
            origem = OrigemRelatorio.DOMINIO;
        } else if (texto.contains("REGISTROS FISCAIS")) {
            origem = OrigemRelatorio.SPED;
        } else {
            origem = OrigemRelatorio.DESCONHECIDA;
        }
        TipoMovimento tipo = (texto.contains("ACOMPANHAMENTO DE ENTRADAS")
                || texto.contains("ENTRADAS DE MERCADORIAS")) ? TipoMovimento.ENTRADA : TipoMovimento.SAIDA;
        return new Identificacao(origem, tipo);
    }

    private String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public record Identificacao(OrigemRelatorio origem, TipoMovimento tipo) {}
}
