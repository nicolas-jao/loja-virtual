package loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Categoria de produtos, com desconto percentual opcional. */
public class Categoria {

    private final long id;
    private final String nome;
    private final String descricao;
    private final BigDecimal percentualDesconto;

    public Categoria(long id, String nome, String descricao, BigDecimal percentualDesconto) {
        if (percentualDesconto.signum() < 0 || percentualDesconto.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentual de desconto deve estar entre 0 e 100");
        }
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.percentualDesconto = percentualDesconto;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPercentualDesconto() { return percentualDesconto; }

    public boolean temDesconto() {
        return percentualDesconto.signum() > 0;
    }

    /** Aplica o desconto da categoria ao preço informado. */
    public BigDecimal aplicarDesconto(BigDecimal preco) {
        BigDecimal desconto = preco.multiply(percentualDesconto).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return preco.subtract(desconto).setScale(2, RoundingMode.HALF_UP);
    }

    public String resumo() {
        return temDesconto() ? nome + " (" + percentualDesconto.stripTrailingZeros().toPlainString() + "% off)" : nome;
    }

    @Override
    public String toString() {
        return resumo();
    }
}
