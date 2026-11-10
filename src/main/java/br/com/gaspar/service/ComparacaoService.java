package br.com.gaspar.service;

import br.com.gaspar.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public final class ComparacaoService {
    public static final BigDecimal TOLERANCIA_VALOR = new BigDecimal("0.01");

    public ResultadoComparacao comparar(
            TipoMovimento tipo,
            List<LinhaSped> linhasSped,
            List<LinhaDominio> linhasDominio,
            boolean arquivosTrocados
    ) {
        Map<ChaveNota, AgrupadoDominio> dominio = agruparDominio(linhasDominio);
        Map<ChaveNota, AgrupadoSped> sped = agruparSped(linhasSped);

        Set<ChaveNota> chaves = new TreeSet<>();
        chaves.addAll(dominio.keySet());
        chaves.addAll(sped.keySet());

        List<NotaComparada> notas = chaves.stream().map(chave -> {
            AgrupadoDominio d = dominio.get(chave);
            AgrupadoSped s = sped.get(chave);
            return new NotaComparada(
                    chave,
                    d == null ? null : d.data,
                    d == null ? null : d.especie,
                    d == null ? null : d.valor,
                    s == null ? null : s.data,
                    s == null ? null : s.modelo,
                    s == null ? null : s.valor,
                    situacao(d, s)
            );
        }).toList();

        return new ResultadoComparacao(tipo, notas, resumir(notas), arquivosTrocados);
    }

    private Map<ChaveNota, AgrupadoDominio> agruparDominio(List<LinhaDominio> linhas) {
        Map<ChaveNota, AgrupadoDominio> resultado = new HashMap<>();
        for (LinhaDominio linha : linhas) {
            resultado.merge(linha.chave(),
                    new AgrupadoDominio(linha.data(), linha.especie(), linha.valorContabil()),
                    (a, b) -> new AgrupadoDominio(a.data, a.especie, a.valor.add(b.valor)));
        }
        return resultado;
    }

    private Map<ChaveNota, AgrupadoSped> agruparSped(List<LinhaSped> linhas) {
        Map<ChaveNota, AgrupadoSped> resultado = new HashMap<>();
        for (LinhaSped linha : linhas) {
            resultado.merge(linha.chave(),
                    new AgrupadoSped(linha.dataEmissao(), linha.modelo(), linha.valorOperacao()),
                    (a, b) -> new AgrupadoSped(a.data, a.modelo, a.valor.add(b.valor)));
        }
        return resultado;
    }

    private SituacaoComparacao situacao(AgrupadoDominio dominio, AgrupadoSped sped) {
        if (dominio == null) return SituacaoComparacao.APENAS_SPED;
        if (sped == null) return SituacaoComparacao.APENAS_DOMINIO;
        if (dominio.valor.subtract(sped.valor).abs().compareTo(TOLERANCIA_VALOR) > 0) {
            return SituacaoComparacao.DIVERGENCIA_VALOR;
        }
        return SituacaoComparacao.OK;
    }

    private ResumoComparacao resumir(List<NotaComparada> notas) {
        long totalDominio = notas.stream().filter(n -> n.valorDominio() != null).count();
        long totalSped = notas.stream().filter(n -> n.valorSped() != null).count();
        long ok = contar(notas, SituacaoComparacao.OK);
        long apenasDominio = contar(notas, SituacaoComparacao.APENAS_DOMINIO);
        long apenasSped = contar(notas, SituacaoComparacao.APENAS_SPED);
        long divergencias = contar(notas, SituacaoComparacao.DIVERGENCIA_VALOR);
        BigDecimal valorDominio = somar(notas.stream().map(NotaComparada::valorDominio).toList());
        BigDecimal valorSped = somar(notas.stream().map(NotaComparada::valorSped).toList());
        return new ResumoComparacao(totalDominio, totalSped, ok, apenasDominio, apenasSped,
                divergencias, valorDominio, valorSped,
                valorDominio.subtract(valorSped).setScale(2, RoundingMode.HALF_UP));
    }

    private long contar(List<NotaComparada> notas, SituacaoComparacao situacao) {
        return notas.stream().filter(n -> n.situacao() == situacao).count();
    }

    private BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record AgrupadoDominio(LocalDate data, String especie, BigDecimal valor) {}
    private record AgrupadoSped(LocalDate data, String modelo, BigDecimal valor) {}
}
