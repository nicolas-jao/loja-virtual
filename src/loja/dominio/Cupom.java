package loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Cupom de desconto, com validade, limite de uso, valor mínimo e nível de fidelidade exigido. */
public class Cupom {

    public enum Tipo {
        PERCENTUAL {
            @Override
            public BigDecimal aplicar(BigDecimal valor, BigDecimal base) {
                return base.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        },
        FIXO {
            @Override
            public BigDecimal aplicar(BigDecimal valor, BigDecimal base) {
                return valor;
            }
        };

        public abstract BigDecimal aplicar(BigDecimal valor, BigDecimal base);
    }

    private final String codigo;
    private final Tipo tipo;
    private final BigDecimal valor;
    private final LocalDate validade;
    private final BigDecimal valorMinimo;
    private final int usoMaximo;
    private final Cliente.Nivel nivelExigido;
    private int usos;
    private boolean ativo = true;

    /**
     * @param validade     último dia de validade, ou null se não expira
     * @param valorMinimo  subtotal mínimo do pedido, ou null se não há mínimo
     * @param usoMaximo    limite de usos, ou 0 para ilimitado
     * @param nivelExigido nível mínimo de fidelidade, ou null se qualquer cliente pode usar
     */
    public Cupom(String codigo, Tipo tipo, BigDecimal valor, LocalDate validade, BigDecimal valorMinimo,
                 int usoMaximo, Cliente.Nivel nivelExigido) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor do cupom deve ser positivo");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.tipo = tipo;
        this.valor = valor;
        this.validade = validade;
        this.valorMinimo = valorMinimo;
        this.usoMaximo = usoMaximo;
        this.nivelExigido = nivelExigido;
    }

    public String getCodigo() { return codigo; }
    public Tipo getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getValidade() { return validade; }
    public BigDecimal getValorMinimo() { return valorMinimo; }
    public int getUsoMaximo() { return usoMaximo; }
    public Cliente.Nivel getNivelExigido() { return nivelExigido; }
    public int getUsos() { return usos; }
    public boolean isAtivo() { return ativo; }

    public void desativar() {
        ativo = false;
    }

    public void registrarUso() {
        usos++;
    }

    public void desfazerUso() {
        usos = Math.max(0, usos - 1);
    }

    public boolean expirado(LocalDate hoje) {
        return validade != null && hoje.isAfter(validade);
    }

    /**
     * Calcula o desconto concedido pelo cupom sobre o subtotal. Retorna zero quando
     * o cupom não se aplica (inativo, expirado, esgotado, abaixo do mínimo ou nível insuficiente).
     */
    public BigDecimal calcularDesconto(BigDecimal subtotal, Cliente cliente, LocalDate hoje) {
        if (!ativo) {
            return BigDecimal.ZERO;
        }
        if (validade != null && hoje.isAfter(validade)) {
            return BigDecimal.ZERO;
        }
        if (usoMaximo > 0 && usos >= usoMaximo) {
            return BigDecimal.ZERO;
        }
        if (valorMinimo != null && subtotal.compareTo(valorMinimo) < 0) {
            return BigDecimal.ZERO;
        }
        if (nivelExigido != null && cliente.getNivel().compareTo(nivelExigido) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal desconto = tipo.aplicar(valor, subtotal);
        return desconto.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    public String descricao() {
        String base = tipo == Tipo.PERCENTUAL
                ? valor.stripTrailingZeros().toPlainString() + "%"
                : "R$ " + valor.setScale(2, RoundingMode.HALF_UP);
        return codigo + " - " + base + " de desconto";
    }

    @Override
    public String toString() {
        return descricao();
    }
}
