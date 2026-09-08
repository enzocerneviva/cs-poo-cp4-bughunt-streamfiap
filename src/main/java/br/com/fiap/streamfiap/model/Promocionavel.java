package br.com.fiap.streamfiap.model;

/**
 * Contrato de promoção do StreamFIAP.
 * Toda classe que implementa esta interface deve aplicar
 * 20% de desconto sobre o preço informado.
 */
public interface Promocionavel {

    /** Fator aplicado ao preço para dar 20% de desconto. */
    double FATOR_DESCONTO_PROMOCIONAL = 0.8;

    double aplicarPromocao(double preco);
}
