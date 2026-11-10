package br.com.gaspar.service;

import br.com.gaspar.domain.NotaComparada;
import br.com.gaspar.domain.ResultadoComparacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class NomeArquivoService {
    private static final String[] MESES = {
            "", "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
            "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
    };

    public String sugerir(ResultadoComparacao resultado) {
        LocalDate menorData = resultado.notas().stream()
                .flatMap(nota -> Stream.of(nota.dataDominio(), nota.dataSped()))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (menorData == null) {
            return "CONFERENCIA_NOTAS_" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)) + ".xlsx";
        }
        return "CONFERENCIA_NOTAS_" + MESES[menorData.getMonthValue()] + "_" + menorData.getYear() + ".xlsx";
    }
}
