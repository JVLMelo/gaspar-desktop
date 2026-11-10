package br.com.gaspar.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record LinhaSped(
        ChaveNota chave,
        String modelo,
        LocalDate dataEmissao,
        BigDecimal valorOperacao
) {
    public LinhaSped {
        Objects.requireNonNull(chave);
        Objects.requireNonNull(dataEmissao);
        Objects.requireNonNull(valorOperacao);
    }
}
