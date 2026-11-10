package br.com.gaspar.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ValorMonetario {
    private ValorMonetario() {}

    public static BigDecimal parseBrasileiro(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Valor monetário vazio");
        }
        String normalizado = valor.trim().replace(".", "").replace(',', '.');
        return new BigDecimal(normalizado).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
