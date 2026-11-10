package br.com.gaspar.domain;

public enum SituacaoComparacao {
    OK("OK"),
    APENAS_DOMINIO("Apenas no Domínio"),
    APENAS_SPED("Apenas no SPED"),
    DIVERGENCIA_VALOR("Divergência de valor");

    private final String descricao;

    SituacaoComparacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
