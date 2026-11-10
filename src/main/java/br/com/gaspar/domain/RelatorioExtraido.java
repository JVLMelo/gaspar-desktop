package br.com.gaspar.domain;

import java.util.List;

public record RelatorioExtraido<T>(TipoMovimento tipo, List<T> linhas) {
    public RelatorioExtraido {
        linhas = List.copyOf(linhas);
    }
}
