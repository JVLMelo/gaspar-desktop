package br.com.gaspar.domain;

public enum TipoMovimento {
    ENTRADA("Entrada"),
    SAIDA("Saída");

    private final String descricao;

    TipoMovimento(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
