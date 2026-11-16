package loja.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import loja.dominio.*;
import loja.servico.LojaService;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** Servidor HTTP da loja (usa apenas o JDK: com.sun.net.httpserver). */
public class Servidor {

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0
                ? Integer.parseInt(args[0])
                : Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        LojaService servico = new LojaService();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/", ex -> {
            try {
                synchronized (servico) {
                    tratar(servico, ex);
                }
            } catch (Exception e) {
                e.printStackTrace();
                enviar(ex, 500, "text/plain; charset=utf-8", "Erro interno: " + e.getMessage());
            }
        });
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();
        System.out.println("Loja Virtual no ar: http://localhost:" + porta);
    }

    private static void tratar(LojaService s, HttpExchange ex) throws IOException {
        String metodo = ex.getRequestMethod();
        String caminho = ex.getRequestURI().getPath();
        Map<String, String> q = parametros(ex.getRequestURI().getRawQuery());

        if ("GET".equals(metodo)) {
            tratarGet(s, ex, caminho, q);
        } else if ("POST".equals(metodo)) {
            Map<String, String> f = parametros(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            tratarPost(s, ex, caminho, f);
        } else {
            enviar(ex, 405, "text/plain; charset=utf-8", "Método não permitido");
        }
    }

    // ------------------------------------------------------------------ GET

    private static void tratarGet(LojaService s, HttpExchange ex, String caminho, Map<String, String> q) throws IOException {
        try {
            if ("/api/produtos".equals(caminho)) {
                enviar(ex, 200, "application/json; charset=utf-8", produtosJson(s));
                return;
            }
            String html = switch (caminho) {
                case "/" -> Paginas.inicio(s, q);
                case "/produtos" -> Paginas.produtos(s, q);
                case "/clientes" -> Paginas.clientes(s, q);
                case "/pedidos" -> Paginas.pedidos(s, q);
                case "/pedido" -> Paginas.pedido(s.buscarPedido(Long.parseLong(q.getOrDefault("id", "0"))), q);
                case "/cupons" -> Paginas.cupons(s, q);
                case "/avaliacoes" -> Paginas.avaliacoes(s, q);
                case "/catalogo" -> Paginas.catalogo(s, q);
                default -> null;
            };
            if (html == null) {
                enviar(ex, 404, "text/html; charset=utf-8", Paginas.naoEncontrada(q));
            } else {
                enviar(ex, 200, "text/html; charset=utf-8", html);
            }
        } catch (IllegalArgumentException e) {
            enviar(ex, 404, "text/html; charset=utf-8", Paginas.layout("Não encontrado", "", Map.of(),
                    "<p>" + Paginas.esc(e.getMessage()) + " <a href=\"/\">Voltar</a></p>"));
        }
    }

    private static String produtosJson(LojaService s) {
        StringBuilder sb = new StringBuilder("[");
        boolean primeiro = true;
        for (Produto p : s.listarProdutos()) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            sb.append("{\"id\":").append(p.getId())
                    .append(",\"nome\":\"").append(json(p.getNome())).append('"')
                    .append(",\"categoria\":\"").append(json(p.getCategoria().getNome())).append('"')
                    .append(",\"preco\":").append(p.precoFinal().toPlainString())
                    .append(",\"estoque\":").append(p.getEstoque().getQuantidade())
                    .append(",\"mediaNotas\":").append(String.format(java.util.Locale.ROOT, "%.2f", p.mediaNotas()))
                    .append('}');
        }
        return sb.append(']').toString();
    }

    private static String json(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ------------------------------------------------------------------ POST

    private static void tratarPost(LojaService s, HttpExchange ex, String caminho, Map<String, String> f) throws IOException {
        String destino = caminho;
        try {
            switch (caminho) {
                case "/produtos" -> {
                    s.cadastrarProduto(f.get("nome"), f.get("descricao"), decimal(f.get("preco")), decimal(f.get("peso")),
                            Long.parseLong(f.get("categoria")), Long.parseLong(f.get("fornecedor")),
                            Integer.parseInt(f.getOrDefault("estoque", "0")));
                    destino = redirecionar("/produtos", "Produto cadastrado com sucesso", false);
                }
                case "/produtos/estoque" -> {
                    s.reabastecer(Long.parseLong(f.get("id")), Integer.parseInt(f.get("qtd")));
                    destino = redirecionar("/produtos", "Estoque atualizado", false);
                }
                case "/clientes" -> {
                    Endereco e = new Endereco(f.get("rua"), f.get("numero"), f.get("bairro"), f.get("cidade"),
                            f.getOrDefault("estado", "").toUpperCase(), f.get("cep"));
                    s.cadastrarCliente(f.get("nome"), f.get("email"), f.get("cpf"), e);
                    destino = redirecionar("/clientes", "Cliente cadastrado com sucesso", false);
                }
                case "/pedidos" -> {
                    Map<Long, Integer> itens = new LinkedHashMap<>();
                    for (int i = 1; i <= 3; i++) {
                        String produto = f.get("produto" + i);
                        if (produto != null && !produto.isBlank()) {
                            itens.merge(Long.parseLong(produto), Integer.parseInt(f.getOrDefault("qtd" + i, "1")), Integer::sum);
                        }
                    }
                    Pedido p = s.criarPedido(Long.parseLong(f.get("cliente")), itens, f.get("cupom"),
                            Frete.Modalidade.valueOf(f.get("modalidade")));
                    destino = redirecionar("/pedido?id=" + p.getId(), "Pedido #" + p.getId() + " criado. Falta o pagamento.", false);
                }
                case "/pedido/pagar" -> {
                    long id = Long.parseLong(f.get("id"));
                    s.pagarPedido(id, Pagamento.Metodo.valueOf(f.get("metodo")), Integer.parseInt(f.getOrDefault("parcelas", "1")));
                    destino = redirecionar("/pedido?id=" + id, "Pagamento aprovado", false);
                }
                case "/pedido/enviar" -> {
                    long id = Long.parseLong(f.get("id"));
                    s.enviarPedido(id);
                    destino = redirecionar("/pedido?id=" + id, "Pedido marcado como enviado", false);
                }
                case "/pedido/entregar" -> {
                    long id = Long.parseLong(f.get("id"));
                    s.entregarPedido(id);
                    destino = redirecionar("/pedido?id=" + id, "Pedido marcado como entregue", false);
                }
                case "/pedido/cancelar" -> {
                    long id = Long.parseLong(f.get("id"));
                    s.cancelarPedido(id);
                    destino = redirecionar("/pedido?id=" + id, "Pedido cancelado e estoque devolvido", false);
                }
                case "/cupons" -> {
                    String validade = f.getOrDefault("validade", "");
                    String minimo = f.getOrDefault("minimo", "");
                    String nivel = f.getOrDefault("nivel", "");
                    s.cadastrarCupom(f.get("codigo"), Cupom.Tipo.valueOf(f.get("tipo")), decimal(f.get("valor")),
                            validade.isBlank() ? null : LocalDate.parse(validade),
                            minimo.isBlank() ? null : decimal(minimo),
                            Integer.parseInt(f.getOrDefault("usoMax", "0")),
                            nivel.isBlank() ? null : Cliente.Nivel.valueOf(nivel));
                    destino = redirecionar("/cupons", "Cupom cadastrado", false);
                }
                case "/avaliacoes" -> {
                    s.avaliar(Long.parseLong(f.get("cliente")), Long.parseLong(f.get("produto")),
                            Integer.parseInt(f.get("nota")), f.get("comentario"));
                    destino = redirecionar("/avaliacoes", "Avaliação registrada", false);
                }
                default -> {
                    enviar(ex, 404, "text/html; charset=utf-8", Paginas.naoEncontrada(Map.of()));
                    return;
                }
            }
        } catch (RuntimeException e) {
            String volta = caminho.startsWith("/pedido/") && f.containsKey("id") ? "/pedido?id=" + f.get("id") : caminho;
            destino = redirecionar(volta, mensagemAmigavel(e), true);
        }
        ex.getResponseHeaders().set("Location", destino);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    private static String mensagemAmigavel(RuntimeException e) {
        if (e instanceof NumberFormatException) {
            return "Valor numérico inválido";
        }
        if (e instanceof java.time.format.DateTimeParseException) {
            return "Data inválida";
        }
        return e.getMessage() == null ? "Não foi possível concluir a operação" : e.getMessage();
    }

    // ------------------------------------------------------------------ infraestrutura HTTP

    private static String redirecionar(String caminho, String mensagem, boolean erro) {
        String separador = caminho.contains("?") ? "&" : "?";
        return caminho + separador + "msg=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8)
                + (erro ? "&tipo=erro" : "");
    }

    private static BigDecimal decimal(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            throw new NumberFormatException("vazio");
        }
        // "1.234,56" (pt-BR) vira "1234.56"; "99.90" é mantido como está
        return new BigDecimal(t.contains(",") ? t.replace(".", "").replace(',', '.') : t);
    }

    private static Map<String, String> parametros(String bruto) {
        Map<String, String> mapa = new LinkedHashMap<>();
        if (bruto == null || bruto.isBlank()) {
            return mapa;
        }
        for (String par : bruto.split("&")) {
            int i = par.indexOf('=');
            String chave = URLDecoder.decode(i < 0 ? par : par.substring(0, i), StandardCharsets.UTF_8);
            String valor = i < 0 ? "" : URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8);
            mapa.put(chave, valor);
        }
        return mapa;
    }

    private static void enviar(HttpExchange ex, int status, String tipo, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
