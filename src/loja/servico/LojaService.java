package loja.servico;

import loja.dominio.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Casos de uso da loja, com persistência em memória e dados de exemplo. */
public final class LojaService {

    private final AtomicLong sequencia = new AtomicLong(100);
    private final Map<Long, Categoria> categorias = new LinkedHashMap<>();
    private final Map<Long, Fornecedor> fornecedores = new LinkedHashMap<>();
    private final Map<Long, Produto> produtos = new LinkedHashMap<>();
    private final Map<Long, Cliente> clientes = new LinkedHashMap<>();
    private final Map<Long, Pedido> pedidos = new LinkedHashMap<>();
    private final Map<String, Cupom> cupons = new LinkedHashMap<>();
    private final List<Avaliacao> avaliacoes = new ArrayList<>();

    public LojaService() {
        popular();
    }

    private long proximoId() {
        return sequencia.incrementAndGet();
    }

    // ---------------------------------------------------------------- consultas

    public Collection<Categoria> listarCategorias() { return categorias.values(); }
    public Collection<Fornecedor> listarFornecedores() { return fornecedores.values(); }
    public Collection<Produto> listarProdutos() { return produtos.values(); }
    public Collection<Cliente> listarClientes() { return clientes.values(); }
    public Collection<Cupom> listarCupons() { return cupons.values(); }
    public List<Avaliacao> listarAvaliacoes() { return avaliacoes; }

    public List<Pedido> listarPedidos() {
        List<Pedido> lista = new ArrayList<>(pedidos.values());
        java.util.Collections.reverse(lista);
        return lista;
    }

    public Produto buscarProduto(long id) {
        Produto p = produtos.get(id);
        if (p == null) {
            throw new IllegalArgumentException("Produto não encontrado: " + id);
        }
        return p;
    }

    public Cliente buscarCliente(long id) {
        Cliente c = clientes.get(id);
        if (c == null) {
            throw new IllegalArgumentException("Cliente não encontrado: " + id);
        }
        return c;
    }

    public Pedido buscarPedido(long id) {
        Pedido p = pedidos.get(id);
        if (p == null) {
            throw new IllegalArgumentException("Pedido não encontrado: " + id);
        }
        return p;
    }

    public Cupom buscarCupom(String codigo) {
        Cupom c = cupons.get(codigo == null ? "" : codigo.trim().toUpperCase());
        if (c == null) {
            throw new IllegalArgumentException("Cupom não encontrado: " + codigo);
        }
        return c;
    }

    // ---------------------------------------------------------------- produtos

    public Produto cadastrarProduto(String nome, String descricao, BigDecimal preco, BigDecimal pesoKg,
                                    long categoriaId, long fornecedorId, int estoqueInicial) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto");
        }
        Categoria categoria = categorias.get(categoriaId);
        Fornecedor fornecedor = fornecedores.get(fornecedorId);
        if (categoria == null || fornecedor == null) {
            throw new IllegalArgumentException("Categoria ou fornecedor inválido");
        }
        Produto p = new Produto(proximoId(), nome.trim(), descricao == null ? "" : descricao.trim(), preco, pesoKg,
                categoria, fornecedor, new Estoque(estoqueInicial, 3));
        produtos.put(p.getId(), p);
        return p;
    }

    public void reabastecer(long produtoId, int unidades) {
        buscarProduto(produtoId).getEstoque().adicionar(unidades);
    }

    // ---------------------------------------------------------------- clientes

    public Cliente cadastrarCliente(String nome, String email, String cpf, Endereco endereco) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do cliente");
        }
        Cliente c = new Cliente(proximoId(), nome.trim(), email, cpf);
        if (!c.emailValido()) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        if (!c.cpfValido()) {
            throw new IllegalArgumentException("CPF inválido");
        }
        c.adicionarEndereco(endereco);
        endereco.regiao(); // valida a UF
        clientes.put(c.getId(), c);
        return c;
    }

    // ---------------------------------------------------------------- cupons

    public Cupom cadastrarCupom(String codigo, Cupom.Tipo tipo, BigDecimal valor, LocalDate validade,
                                BigDecimal valorMinimo, int usoMaximo, Cliente.Nivel nivel) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Informe o código do cupom");
        }
        Cupom c = new Cupom(codigo, tipo, valor, validade, valorMinimo, usoMaximo, nivel);
        if (cupons.containsKey(c.getCodigo())) {
            throw new IllegalArgumentException("Já existe um cupom com o código " + c.getCodigo());
        }
        cupons.put(c.getCodigo(), c);
        return c;
    }

    // ---------------------------------------------------------------- pedidos

    /** Cria um pedido com os itens (produtoId -> quantidade) e, opcionalmente, um cupom. */
    public Pedido criarPedido(long clienteId, Map<Long, Integer> itens, String codigoCupom,
                              Frete.Modalidade modalidade) {
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um produto");
        }
        Pedido pedido = new Pedido(proximoId(), buscarCliente(clienteId), modalidade);
        try {
            for (Map.Entry<Long, Integer> e : itens.entrySet()) {
                pedido.adicionarItem(buscarProduto(e.getKey()), e.getValue());
            }
            if (codigoCupom != null && !codigoCupom.isBlank()) {
                pedido.aplicarCupom(buscarCupom(codigoCupom));
            }
        } catch (RuntimeException ex) {
            pedido.cancelar(); // devolve ao estoque o que já havia sido reservado
            throw ex;
        }
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    public void pagarPedido(long pedidoId, Pagamento.Metodo metodo, int parcelas) {
        buscarPedido(pedidoId).pagar(proximoId(), metodo, parcelas);
    }

    public void enviarPedido(long pedidoId) { buscarPedido(pedidoId).enviar(); }
    public void entregarPedido(long pedidoId) { buscarPedido(pedidoId).entregar(); }
    public void cancelarPedido(long pedidoId) { buscarPedido(pedidoId).cancelar(); }

    // ---------------------------------------------------------------- avaliações

    /** Somente clientes com um pedido pago contendo o produto podem avaliá-lo. */
    public Avaliacao avaliar(long clienteId, long produtoId, int nota, String comentario) {
        Cliente cliente = buscarCliente(clienteId);
        Produto produto = buscarProduto(produtoId);
        boolean comprou = pedidos.values().stream()
                .anyMatch(p -> p.getCliente() == cliente && p.foiPago() && p.contem(produto));
        if (!comprou) {
            throw new IllegalStateException(cliente.getNome() + " ainda não comprou " + produto.getNome());
        }
        Avaliacao a = new Avaliacao(proximoId(), cliente, produto, nota, comentario);
        produto.adicionarAvaliacao(a);
        avaliacoes.add(a);
        return a;
    }

    // ---------------------------------------------------------------- dados de exemplo

    private void popular() {
        Categoria eletronicos = novaCategoria("Eletrônicos", "Computadores, áudio e acessórios", "5");
        Categoria livros = novaCategoria("Livros", "Livros técnicos e de ficção", "0");
        Categoria casa = novaCategoria("Casa e Cozinha", "Utilidades domésticas", "10");

        Fornecedor tech = novoFornecedor("TechSul Distribuidora", "12.345.678/0001-90", "contato@techsul.example", 5);
        Fornecedor editora = novoFornecedor("Editora Horizonte", "98.765.432/0001-10", "vendas@horizonte.example", 7);
        Fornecedor lar = novoFornecedor("Lar & Cia", "11.222.333/0001-44", "atendimento@larecia.example", 4);

        novoProduto("Notebook 14\"", "Notebook 16 GB RAM, SSD 512 GB", "3499.90", "1.8", eletronicos, tech, 8);
        Produto fone = novoProduto("Fone Bluetooth", "Fone sem fio com cancelamento de ruído", "299.90", "0.3", eletronicos, tech, 25);
        novoProduto("Teclado Mecânico", "Teclado ABNT2 com switches azuis", "259.00", "0.9", eletronicos, tech, 15);
        Produto cleanCode = novoProduto("Clean Code", "Livro de Robert C. Martin", "89.90", "0.6", livros, editora, 30);
        novoProduto("Refactoring", "Livro de Martin Fowler", "119.90", "0.7", livros, editora, 12);
        novoProduto("Jogo de Panelas", "Conjunto antiaderente com 5 peças", "329.90", "6.5", casa, lar, 10);
        novoProduto("Cafeteira Elétrica", "Cafeteira com jarra de vidro 1,2 L", "139.90", "1.7", casa, lar, 2);

        Cliente ana = novoCliente("Ana Souza", "ana@example.com", "529.982.247-25",
                new Endereco("Rua das Flores", "120", "Centro", "Juiz de Fora", "MG", "36010-000"));
        Cliente bruno = novoCliente("Bruno Lima", "bruno@example.com", "111.444.777-35",
                new Endereco("Av. Paulista", "1000", "Bela Vista", "São Paulo", "SP", "01310-100"));
        novoCliente("Carla Mendes", "carla@example.com", "390.533.447-05",
                new Endereco("Rua do Sol", "45", "Aldeota", "Fortaleza", "CE", "60125-000"));
        ana.adicionarPontos(2500);

        LocalDate hoje = LocalDate.now();
        cupons.put("BEMVINDO10", new Cupom("BEMVINDO10", Cupom.Tipo.PERCENTUAL, new BigDecimal("10"),
                hoje.plusMonths(6), new BigDecimal("50"), 0, null));
        cupons.put("FIXO30", new Cupom("FIXO30", Cupom.Tipo.FIXO, new BigDecimal("30"),
                null, new BigDecimal("200"), 100, null));
        cupons.put("OURO15", new Cupom("OURO15", Cupom.Tipo.PERCENTUAL, new BigDecimal("15"),
                null, null, 0, Cliente.Nivel.OURO));
        cupons.put("VERAO", new Cupom("VERAO", Cupom.Tipo.PERCENTUAL, new BigDecimal("20"),
                hoje.minusDays(30), null, 0, null));

        Pedido p1 = criarPedido(ana.getId(), umItem(cleanCode.getId(), 1), null, Frete.Modalidade.PADRAO);
        pagarPedido(p1.getId(), Pagamento.Metodo.PIX, 1);
        enviarPedido(p1.getId());
        entregarPedido(p1.getId());
        avaliar(ana.getId(), cleanCode.getId(), 5, "Leitura obrigatória, muito bem escrita.");

        Pedido p2 = criarPedido(bruno.getId(), umItem(fone.getId(), 1), "BEMVINDO10", Frete.Modalidade.EXPRESSO);
        pagarPedido(p2.getId(), Pagamento.Metodo.CARTAO_CREDITO, 3);
    }

    private static Map<Long, Integer> umItem(long produtoId, int quantidade) {
        Map<Long, Integer> m = new LinkedHashMap<>();
        m.put(produtoId, quantidade);
        return m;
    }

    private Categoria novaCategoria(String nome, String descricao, String desconto) {
        Categoria c = new Categoria(proximoId(), nome, descricao, new BigDecimal(desconto));
        categorias.put(c.getId(), c);
        return c;
    }

    private Fornecedor novoFornecedor(String nome, String cnpj, String email, int prazo) {
        Fornecedor f = new Fornecedor(proximoId(), nome, cnpj, email, prazo);
        fornecedores.put(f.getId(), f);
        return f;
    }

    private Produto novoProduto(String nome, String descricao, String preco, String peso,
                                Categoria categoria, Fornecedor fornecedor, int estoque) {
        Produto p = new Produto(proximoId(), nome, descricao, new BigDecimal(preco), new BigDecimal(peso),
                categoria, fornecedor, new Estoque(estoque, 3));
        produtos.put(p.getId(), p);
        return p;
    }

    private Cliente novoCliente(String nome, String email, String cpf, Endereco endereco) {
        Cliente c = new Cliente(proximoId(), nome, email, cpf);
        c.adicionarEndereco(endereco);
        clientes.put(c.getId(), c);
        return c;
    }
}
