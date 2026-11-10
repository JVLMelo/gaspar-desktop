package br.com.gaspar.domain;

import java.util.List;

public record ResultadoComparacao(
        TipoMovimento tipoMovimento,
        List<NotaComparada> notas,
        ResumoComparacao resumo,
        boolean arquivosTrocados
) {
    public ResultadoComparacao {
        notas = List.copyOf(notas);
    }
}
