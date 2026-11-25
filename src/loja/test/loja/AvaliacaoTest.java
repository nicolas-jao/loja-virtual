package loja;

import loja.dominio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Avaliacao.validar()")
class AvaliacaoTest {

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void setUp() {
        cliente = new Cliente(1L, "Ana Souza", "ana@example.com", "529.982.247-25");
        Categoria categoria = new Categoria(1L, "Livros", "Livros técnicos", BigDecimal.ZERO);
        Fornecedor fornecedor = new Fornecedor(1L, "Editora", "12.345.678/0001-90", "ed@example.com", 5);
        produto = new Produto(1L, "Clean Code", "Livro", new BigDecimal("89.90"),
                new BigDecimal("0.6"), categoria, fornecedor, new Estoque(10, 2));
    }

    @ParameterizedTest(name = "Nota {0} deve ser aceita na validação")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("Deve validar avaliação com cliente, produto, comentário válido e nota entre 1 e 5")
    void deveValidarAvaliacaoComNotasValidas(int nota) {
        Avaliacao avaliacao = assertDoesNotThrow(() ->
                new Avaliacao(1L, cliente, produto, nota, "Excelente produto"));

        assertDoesNotThrow(avaliacao::validar);
        assertEquals(nota, avaliacao.getNota());
    }

    @Test
    @DisplayName("Deve aceitar comentário nulo convertendo-o para string vazia")
    void deveAceitarComentarioNulo() {
        Avaliacao avaliacao = assertDoesNotThrow(() ->
                new Avaliacao(1L, cliente, produto, 4, null));

        assertDoesNotThrow(avaliacao::validar);
        assertEquals("", avaliacao.getComentario());
    }

    @Test
    @DisplayName("Deve aceitar comentário vazio ou apenas com espaços")
    void deveAceitarComentarioEmBranco() {
        Avaliacao avaliacao = assertDoesNotThrow(() ->
                new Avaliacao(1L, cliente, produto, 3, "    "));

        assertDoesNotThrow(avaliacao::validar);
        assertEquals("", avaliacao.getComentario());
    }

    @Test
    @DisplayName("Deve aceitar comentário com exatamente 500 caracteres (fronteira)")
    void deveAceitarComentarioComExatamente500Caracteres() {
        String comentario500 = "a".repeat(500);

        Avaliacao avaliacao = assertDoesNotThrow(() ->
                new Avaliacao(1L, cliente, produto, 5, comentario500));

        assertDoesNotThrow(avaliacao::validar);
        assertEquals(500, avaliacao.getComentario().length());
    }

    @Test
    @DisplayName("Deve aceitar comentário com 500 caracteres após remoção de espaços nas extremidades")
    void deveAceitarComentarioCom500CaracteresAposTrim() {
        String comentarioComEspacos = "   " + "a".repeat(500) + "   ";

        Avaliacao avaliacao = assertDoesNotThrow(() ->
                new Avaliacao(1L, cliente, produto, 5, comentarioComEspacos));

        assertDoesNotThrow(avaliacao::validar);
        assertEquals(500, avaliacao.getComentario().length());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o comentário exceder 500 caracteres")
    void deveRejeitarComentarioAcimaDe500Caracteres() {
        String comentario501 = "a".repeat(501);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Avaliacao(1L, cliente, produto, 5, comentario501));

        assertEquals("Comentário muito longo (máximo 500 caracteres)", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o cliente for nulo")
    void deveRejeitarClienteNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Avaliacao(1L, null, produto, 5, "Ótimo"));

        assertEquals("Avaliação precisa de cliente e produto", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o produto for nulo")
    void deveRejeitarProdutoNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Avaliacao(1L, cliente, null, 5, "Ótimo"));

        assertEquals("Avaliação precisa de cliente e produto", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando cliente e produto forem nulos")
    void deveRejeitarClienteEProdutoNulos() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Avaliacao(1L, null, null, 5, "Ótimo"));

        assertEquals("Avaliação precisa de cliente e produto", ex.getMessage());
    }

    @ParameterizedTest(name = "Nota inválida {0} deve lançar IllegalArgumentException")
    @ValueSource(ints = {Integer.MIN_VALUE, -5, 0, 6, 10, Integer.MAX_VALUE})
    @DisplayName("Deve lançar IllegalArgumentException quando a nota estiver fora do intervalo de 1 a 5")
    void deveRejeitarNotasForaDoIntervalo(int notaInvalida) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Avaliacao(1L, cliente, produto, notaInvalida, "Comentário"));

        assertEquals("A nota deve estar entre 1 e 5", ex.getMessage());
    }
}
