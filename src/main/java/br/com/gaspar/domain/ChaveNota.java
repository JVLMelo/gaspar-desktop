package br.com.gaspar.domain;

public record ChaveNota(long nota, int serie) implements Comparable<ChaveNota> {
    public ChaveNota {
        if (nota < 0) throw new IllegalArgumentException("Número da nota não pode ser negativo");
        if (serie < 0) throw new IllegalArgumentException("Série não pode ser negativa");
    }

    @Override
    public int compareTo(ChaveNota outra) {
        int porNota = Long.compare(nota, outra.nota);
        return porNota != 0 ? porNota : Integer.compare(serie, outra.serie);
    }
}
