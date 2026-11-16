package loja.dominio;

/** Controle de estoque de um produto. */
public class Estoque {

    private int quantidade;
    private final int minimo;

    public Estoque(int quantidadeInicial, int minimo) {
        if (quantidadeInicial < 0 || minimo < 0) {
            throw new IllegalArgumentException("Quantidades não podem ser negativas");
        }
        this.quantidade = quantidadeInicial;
        this.minimo = minimo;
    }

    public int getQuantidade() { return quantidade; }
    public int getMinimo() { return minimo; }

    public boolean disponivel(int pedida) {
        return pedida > 0 && quantidade >= pedida;
    }

    public void adicionar(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("Quantidade a adicionar deve ser positiva");
        }
        quantidade += unidades;
    }

    public void retirar(int unidades) {
        if (!disponivel(unidades)) {
            throw new IllegalStateException("Estoque insuficiente (disponível: " + quantidade + ")");
        }
        quantidade -= unidades;
    }

    public boolean abaixoDoMinimo() {
        return quantidade <= minimo;
    }

    @Override
    public String toString() {
        return quantidade + " un." + (abaixoDoMinimo() ? " (reposição necessária)" : "");
    }
}
