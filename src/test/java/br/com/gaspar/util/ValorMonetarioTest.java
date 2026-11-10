package br.com.gaspar.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValorMonetarioTest {
    @Test
    void converteFormatoBrasileiro() {
        assertEquals(new BigDecimal("1234.56"), ValorMonetario.parseBrasileiro("1.234,56"));
        assertEquals(new BigDecimal("-7.10"), ValorMonetario.parseBrasileiro("-7,10"));
        assertEquals(new BigDecimal("0.00"), ValorMonetario.parseBrasileiro("0,00"));
    }
}
