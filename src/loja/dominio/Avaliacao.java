package loja.dominio;

import java.time.LocalDate;

/** Avaliação (nota e comentário) de um cliente sobre um produto. */
public class Avaliacao {

    private final long id;
    private final Cliente cliente;
    private final Produto produto;
    private final int nota;
    private final String comentario;
    private final LocalDate data = LocalDate.now();

    public Avaliacao(long id, Cliente cliente, Produto produto, int nota, String comentario) {
        this.id = id;
        this.cliente = cliente;
        this.produto = produto;
        this.nota = nota;
        this.comentario = comentario == null ? "" : comentario.trim();
        validar();
    }

    public long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Produto getProduto() { return produto; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDate getData() { return data; }

    /** Garante que a avaliação tem autor, produto e nota entre 1 e 5. */
    public final void validar() {
        if (cliente == null || produto == null) {
            throw new IllegalArgumentException("Avaliação precisa de cliente e produto");}
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5");}
        if (comentario.length() > 500) {
            throw new IllegalArgumentException("Comentário muito longo (máximo 500 caracteres)");}
    }

    public String estrelas() {
        return "★".repeat(nota) + "☆".repeat(5 - nota);
    }

    public String resumo() {
        return estrelas() + " " + cliente.getNome() + ": " + comentario;
    }

    @Override
    public String toString() {
        return resumo();
    }
}
