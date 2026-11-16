package loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Pagamento de um pedido. */
public class Pagamento {

    public enum Metodo { PIX, CARTAO_CREDITO, BOLETO }

    public enum Status { PENDENTE, APROVADO, ESTORNADO }

    private static final BigDecimal PARCELA_MINIMA = new BigDecimal("5.00");

    private final long id;
    private final long pedidoId;
    private final Metodo metodo;
    private final BigDecimal valor;
    private final int parcelas;
    private Status status = Status.PENDENTE;
    private LocalDateTime processadoEm;

    public Pagamento(long id, long pedidoId, Metodo metodo, BigDecimal valor, int parcelas) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.metodo = metodo;
        this.valor = valor;
        this.parcelas = parcelas;
    }

    public long getId() { return id; }
    public long getPedidoId() { return pedidoId; }
    public Metodo getMetodo() { return metodo; }
    public BigDecimal getValor() { return valor; }
    public int getParcelas() { return parcelas; }
    public Status getStatus() { return status; }
    public LocalDateTime getProcessadoEm() { return processadoEm; }

    public BigDecimal valorParcela() {
        return valor.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
    }

    /** Valida as regras do método de pagamento e aprova o pagamento. */
    public void processar() {
        if (status != Status.PENDENTE) {
            throw new IllegalStateException("Pagamento já foi processado");
        }
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser positivo");
        }
        if (metodo == Metodo.CARTAO_CREDITO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new IllegalArgumentException("Parcelamento permitido: de 1 a 12 vezes");
            }
            if (valorParcela().compareTo(PARCELA_MINIMA) < 0) {
                throw new IllegalArgumentException("Parcela mínima de R$ 5,00");
            }
        } else if (parcelas != 1) {
            throw new IllegalArgumentException("Este método de pagamento só permite pagamento à vista");
        }
        status = Status.APROVADO;
        processadoEm = LocalDateTime.now();
    }

    public void estornar() {
        if (status != Status.APROVADO) {
            throw new IllegalStateException("Somente pagamentos aprovados podem ser estornados");
        }
        status = Status.ESTORNADO;
    }

    @Override
    public String toString() {
        return metodo + " " + parcelas + "x - " + status;
    }
}
