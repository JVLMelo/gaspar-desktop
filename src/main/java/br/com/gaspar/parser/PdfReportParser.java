package br.com.gaspar.parser;

import br.com.gaspar.domain.RelatorioExtraido;
import br.com.gaspar.exception.ReportParsingException;

import java.nio.file.Path;

public interface PdfReportParser<T> {
    RelatorioExtraido<T> parse(Path arquivo) throws ReportParsingException;
}
