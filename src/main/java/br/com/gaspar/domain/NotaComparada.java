package br.com.gaspar.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record NotaComparada(
        ChaveNota chave,
        LocalDate dataDominio,
        String especieDominio,
        BigDecimal valorDominio,
        LocalDate dataSped,
        String modeloSped,
        BigDecimal valorSped,
        SituacaoComparacao situacao
) {
    public NotaComparada {
        Objects.requireNonNull(chave);
        Objects.requireNonNull(situacao);
    }
}
