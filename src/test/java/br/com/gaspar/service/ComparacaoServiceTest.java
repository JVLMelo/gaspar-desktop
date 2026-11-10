package br.com.gaspar.service;

import br.com.gaspar.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComparacaoServiceTest {
    private final ComparacaoService service = new ComparacaoService();
    private final LocalDate date = LocalDate.of(2026, 7, 1);

    @Test
    void agrupaCfopsEClassificaTodasAsSituacoes() {
        List<LinhaDominio> dominio = List.of(
                dominio(1, 1, "10.00"),
                dominio(1, 1, "5.00"),
                dominio(2, 1, "20.00"),
                dominio(4, 1, "30.00")
        );
        List<LinhaSped> sped = List.of(
                sped(1, 1, "15.00"),
                sped(2, 1, "20.02"),
                sped(3, 1, "40.00")
        );

        ResultadoComparacao result = service.comparar(TipoMovimento.SAIDA, sped, dominio, false);

        assertEquals(List.of(
                SituacaoComparacao.OK,
                SituacaoComparacao.DIVERGENCIA_VALOR,
                SituacaoComparacao.APENAS_SPED,
                SituacaoComparacao.APENAS_DOMINIO
        ), result.notas().stream().map(NotaComparada::situacao).toList());
        assertEquals(new BigDecimal("65.00"), result.resumo().valorTotalDominio());
        assertEquals(new BigDecimal("75.02"), result.resumo().valorTotalSped());
        assertEquals(new BigDecimal("-10.02"), result.resumo().diferenca());
    }

    @Test
    void diferencaDeUmCentavoContinuaOk() {
        ResultadoComparacao result = service.comparar(TipoMovimento.ENTRADA,
                List.of(sped(10, 2, "100.01")), List.of(dominio(10, 2, "100.00")), false);
        assertEquals(SituacaoComparacao.OK, result.notas().getFirst().situacao());
    }

    @Test
    void seriesDiferentesNaoSaoMesmaNota() {
        ResultadoComparacao result = service.comparar(TipoMovimento.SAIDA,
                List.of(sped(10, 2, "10.00")), List.of(dominio(10, 1, "10.00")), false);
        assertEquals(2, result.notas().size());
        assertEquals(1, result.resumo().apenasDominio());
        assertEquals(1, result.resumo().apenasSped());
    }

    private LinhaDominio dominio(long note, int series, String value) {
        return new LinhaDominio(new ChaveNota(note, series), "55", date, new BigDecimal(value));
    }

    private LinhaSped sped(long note, int series, String value) {
        return new LinhaSped(new ChaveNota(note, series), "55", date, new BigDecimal(value));
    }
}
