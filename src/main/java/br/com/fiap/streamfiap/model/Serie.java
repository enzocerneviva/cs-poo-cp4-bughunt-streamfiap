package br.com.fiap.streamfiap.model;

import jakarta.persistence.Entity;

@Entity
public class Serie extends Conteudo implements Promocionavel {

    private static final double PRECO_POR_TEMPORADA = 4.90;

    private int numeroTemporadas;

    public Serie() {
    }

    // cria a série com os dados recebidos
    public Serie(String titulo, String categoria, int duracaoMinutos, int classificacaoEtaria, int numeroTemporadas) {
        super(titulo, categoria, duracaoMinutos, classificacaoEtaria, true);
        this.numeroTemporadas = numeroTemporadas;
    }

    @Override
    public double calcularPrecoAluguel() {
        return PRECO_POR_TEMPORADA * numeroTemporadas;
    }

    @Override
    public double aplicarPromocao(double preco) {
        return preco * FATOR_DESCONTO_PROMOCIONAL;
    }

    public int getNumeroTemporadas() { return numeroTemporadas; }
    public void setNumeroTemporadas(int numeroTemporadas) { this.numeroTemporadas = numeroTemporadas; }
}
