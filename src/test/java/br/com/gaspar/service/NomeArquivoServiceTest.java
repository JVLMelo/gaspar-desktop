package br.com.gaspar.service;

import br.com.gaspar.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NomeArquivoServiceTest {
    @Test
    void usaMenorDataDisponivel() {
        NotaComparada nota = new NotaComparada(new ChaveNota(1, 1),
                LocalDate.of(2026, 7, 10), "55", BigDecimal.ONE,
                LocalDate.of(2026, 7, 1), "55", BigDecimal.ONE, SituacaoComparacao.OK);
        ResultadoComparacao result = new ResultadoComparacao(TipoMovimento.SAIDA, List.of(nota),
                new ResumoComparacao(1, 1, 1, 0, 0, 0, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO), false);
        assertEquals("CONFERENCIA_NOTAS_JULHO_2026.xlsx", new NomeArquivoService().sugerir(result));
    }
}
