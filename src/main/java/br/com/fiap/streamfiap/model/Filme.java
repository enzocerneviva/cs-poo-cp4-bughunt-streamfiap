package br.com.fiap.streamfiap.model;

import jakarta.persistence.Entity;

@Entity
public class Filme extends Conteudo implements Promocionavel {

    private static final double PRECO_BASE = 9.90;
    private static final double ADICIONAL_ESTREIA = 5.00;

    private boolean estreia;

    public Filme() {
    }

    public Filme(String titulo, String categoria, int duracaoMinutos, int classificacaoEtaria, boolean disponivel, boolean estreia) {
        super(titulo, categoria, duracaoMinutos, classificacaoEtaria, disponivel);
        this.estreia = estreia;
    }

    @Override
    public double calcularPrecoAluguel() {
        return PRECO_BASE + (estreia ? ADICIONAL_ESTREIA : 0.0);
    }

    @Override
    public double aplicarPromocao(double preco) {
        return preco * FATOR_DESCONTO_PROMOCIONAL;
    }

    public boolean isEstreia() { return estreia; }
    public void setEstreia(boolean estreia) { this.estreia = estreia; }
}
