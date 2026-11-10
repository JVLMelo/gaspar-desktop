package br.com.gaspar.domain;

import java.math.BigDecimal;

public record ResumoComparacao(
        long notasDominio,
        long notasSped,
        long iguais,
        long apenasDominio,
        long apenasSped,
        long divergencias,
        BigDecimal valorTotalDominio,
        BigDecimal valorTotalSped,
        BigDecimal diferenca
) {}
