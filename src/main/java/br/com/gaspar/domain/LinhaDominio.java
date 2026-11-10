package br.com.gaspar.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record LinhaDominio(
        ChaveNota chave,
        String especie,
        LocalDate data,
        BigDecimal valorContabil
) {
    public LinhaDominio {
        Objects.requireNonNull(chave);
        Objects.requireNonNull(data);
        Objects.requireNonNull(valorContabil);
    }
}
