package loja.dominio;

import java.time.LocalDate;

/** Fornecedor de produtos. */
public class Fornecedor {

    private final long id;
    private final String nome;
    private final String cnpj;
    private final String email;
    private final int prazoEntregaDias;
    private boolean ativo = true;

    public Fornecedor(long id, String nome, String cnpj, String email, int prazoEntregaDias) {
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
        this.email = email;
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public String getEmail() { return email; }
    public int getPrazoEntregaDias() { return prazoEntregaDias; }
    public boolean isAtivo() { return ativo; }

    public void desativar() {
        ativo = false;
    }

    public void reativar() {
        ativo = true;
    }

    public boolean cnpjValido() {
        return cnpj != null && cnpj.replaceAll("\\D", "").length() == 14;
    }

    /** Data estimada em que o fornecedor entrega um reabastecimento pedido na data informada. */
    public LocalDate estimarReabastecimento(LocalDate dataPedido) {
        return dataPedido.plusDays(prazoEntregaDias);
    }

    @Override
    public String toString() {
        return nome + " (" + prazoEntregaDias + " dias)";
    }
}
