package loja.dominio;

import java.math.BigDecimal;

/** Item de um pedido: produto, quantidade e o preço praticado no momento da compra. */
public class ItemPedido {

    private final Produto produto;
    private final BigDecimal precoUnitario;
    private int quantidade;

    public ItemPedido(Produto produto, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        this.produto = produto;
        this.precoUnitario = produto.precoFinal();
        this.quantidade = quantidade;
    }

    public Produto getProduto() { return produto; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getQuantidade() { return quantidade; }

    public void somar(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        quantidade += unidades;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return produto.getPesoKg().multiply(BigDecimal.valueOf(quantidade));
    }

    @Override
    public String toString() {
        return quantidade + "x " + produto.getNome();
    }
}
