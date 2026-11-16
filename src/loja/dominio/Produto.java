package loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Produto à venda na loja. */
public class Produto {

    private final long id;
    private final String nome;
    private final String descricao;
    private BigDecimal preco;
    private final BigDecimal pesoKg;
    private final Categoria categoria;
    private final Fornecedor fornecedor;
    private final Estoque estoque;
    private final List<Avaliacao> avaliacoes = new ArrayList<>();
    private boolean ativo = true;

    public Produto(long id, String nome, String descricao, BigDecimal preco, BigDecimal pesoKg,
                   Categoria categoria, Fornecedor fornecedor, Estoque estoque) {
        if (preco.signum() <= 0) {
            throw new IllegalArgumentException("Preço deve ser positivo");
        }
        if (pesoKg.signum() <= 0) {
            throw new IllegalArgumentException("Peso deve ser positivo");
        }
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco.setScale(2, RoundingMode.HALF_UP);
        this.pesoKg = pesoKg;
        this.categoria = categoria;
        this.fornecedor = fornecedor;
        this.estoque = estoque;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public Categoria getCategoria() { return categoria; }
    public Fornecedor getFornecedor() { return fornecedor; }
    public Estoque getEstoque() { return estoque; }
    public boolean isAtivo() { return ativo; }

    public List<Avaliacao> getAvaliacoes() {
        return Collections.unmodifiableList(avaliacoes);
    }

    /** Preço já com o desconto da categoria. */
    public BigDecimal precoFinal() {
        return categoria.aplicarDesconto(preco);
    }

    public void alterarPreco(BigDecimal novoPreco) {
        if (novoPreco.signum() <= 0) {
            throw new IllegalArgumentException("Preço deve ser positivo");
        }
        this.preco = novoPreco.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean disponivel(int quantidade) {
        return ativo && estoque.disponivel(quantidade);
    }

    public void desativar() {
        ativo = false;
    }

    public void adicionarAvaliacao(Avaliacao avaliacao) {
        avaliacoes.add(avaliacao);
    }

    /** Média das notas recebidas, ou zero se não há avaliações. */
    public double mediaNotas() {
        return avaliacoes.stream().mapToInt(Avaliacao::getNota).average().orElse(0.0);
    }

    @Override
    public String toString() {
        return nome;
    }
}
