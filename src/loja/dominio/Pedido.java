package loja.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Pedido de compra: itens, cupom, frete, pagamento e ciclo de vida. */
public class Pedido {

    public enum Status { CRIADO, PAGO, ENVIADO, ENTREGUE, CANCELADO }

    private final long id;
    private final Cliente cliente;
    private final Endereco enderecoEntrega;
    private final Frete.Modalidade modalidade;
    private final Frete calculadoraFrete = new Frete();
    private final List<ItemPedido> itens = new ArrayList<>();
    private final LocalDateTime criadoEm = LocalDateTime.now();
    private Status status = Status.CRIADO;
    private Cupom cupom;
    private Pagamento pagamento;

    public Pedido(long id, Cliente cliente, Frete.Modalidade modalidade) {
        this.id = id;
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.modalidade = Objects.requireNonNull(modalidade, "modalidade");
        this.enderecoEntrega = cliente.enderecoPrincipal();
    }

    public long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Endereco getEnderecoEntrega() { return enderecoEntrega; }
    public Frete.Modalidade getModalidade() { return modalidade; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Status getStatus() { return status; }
    public Cupom getCupom() { return cupom; }
    public Pagamento getPagamento() { return pagamento; }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    private void exigirStatus(Status esperado) {
        if (status != esperado) {
            throw new IllegalStateException("Operação não permitida: o pedido está " + status);
        }
    }

    /** Adiciona um item e reserva as unidades no estoque. */
    public void adicionarItem(Produto produto, int quantidade) {
        exigirStatus(Status.CRIADO);
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (!produto.disponivel(quantidade)) {
            throw new IllegalStateException("Produto indisponível: " + produto.getNome());
        }
        produto.getEstoque().retirar(quantidade);
        for (ItemPedido item : itens) {
            if (item.getProduto() == produto) {
                item.somar(quantidade);
                return;
            }
        }
        itens.add(new ItemPedido(produto, quantidade));
    }

    public void aplicarCupom(Cupom novoCupom) {
        exigirStatus(Status.CRIADO);
        if (novoCupom.calcularDesconto(subtotal(), cliente, LocalDate.now()).signum() == 0) {
            throw new IllegalArgumentException("O cupom " + novoCupom.getCodigo() + " não é aplicável a este pedido");
        }
        this.cupom = novoCupom;
    }

    public boolean contem(Produto produto) {
        return itens.stream().anyMatch(i -> i.getProduto() == produto);
    }

    public boolean foiPago() {
        return status == Status.PAGO || status == Status.ENVIADO || status == Status.ENTREGUE;
    }

    public BigDecimal subtotal() {
        return itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream().map(ItemPedido::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal desconto() {
        if (cupom == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return cupom.calcularDesconto(subtotal(), cliente, LocalDate.now());
    }

    public BigDecimal valorFrete() {
        if (itens.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return calculadoraFrete.calcular(enderecoEntrega, pesoTotal(), subtotal().subtract(desconto()),
                modalidade, cliente.getNivel() == Cliente.Nivel.OURO);
    }

    public int prazoEstimadoDias() {
        return calculadoraFrete.prazoEstimadoDias(enderecoEntrega, modalidade);
    }

    public BigDecimal total() {
        return subtotal().subtract(desconto()).add(valorFrete());
    }

    /** Processa o pagamento, registra o uso do cupom e credita pontos de fidelidade (1 por real). */
    public void pagar(long idPagamento, Pagamento.Metodo metodo, int parcelas) {
        exigirStatus(Status.CRIADO);
        if (itens.isEmpty()) {
            throw new IllegalStateException("Pedido sem itens");
        }
        BigDecimal valorTotal = total();
        Pagamento novo = new Pagamento(idPagamento, id, metodo, valorTotal, parcelas);
        novo.processar();
        this.pagamento = novo;
        if (cupom != null) {
            cupom.registrarUso();
        }
        cliente.adicionarPontos(valorTotal.intValue());
        status = Status.PAGO;
    }

    public void enviar() {
        exigirStatus(Status.PAGO);
        status = Status.ENVIADO;
    }

    public void entregar() {
        exigirStatus(Status.ENVIADO);
        status = Status.ENTREGUE;
    }

    public boolean podeCancelar() {
        return status == Status.CRIADO || status == Status.PAGO;
    }

    /** Cancela o pedido: devolve o estoque, estorna o pagamento e desfaz pontos e uso do cupom. */
    public void cancelar() {
        if (!podeCancelar()) {
            throw new IllegalStateException("Pedido " + status + " não pode ser cancelado");
        }
        for (ItemPedido item : itens) {
            item.getProduto().getEstoque().adicionar(item.getQuantidade());
        }
        if (pagamento != null) {
            pagamento.estornar();
            cliente.removerPontos(pagamento.getValor().intValue());
            if (cupom != null) {
                cupom.desfazerUso();
            }
        }
        status = Status.CANCELADO;
    }

    @Override
    public String toString() {
        return "Pedido #" + id + " (" + status + ")";
    }
}
