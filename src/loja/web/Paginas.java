package loja.web;

import loja.dominio.*;
import loja.servico.LojaService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Geração do HTML das páginas (renderização no servidor). */
final class Paginas {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Paginas() {
    }

    // ------------------------------------------------------------------ utilidades

    static String esc(Object o) {
        if (o == null) {
            return "";
        }
        return o.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String moeda(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(v);
    }

    private static String selo(String texto, String classe) {
        return "<span class=\"selo " + classe + "\">" + esc(texto) + "</span>";
    }

    private static String seloStatus(Pedido.Status s) {
        String classe = switch (s) {
            case CRIADO -> "cinza";
            case PAGO -> "azul";
            case ENVIADO -> "amarelo";
            case ENTREGUE -> "verde";
            case CANCELADO -> "vermelho";
        };
        return selo(s.name(), classe);
    }

    private static String optionsClientes(LojaService s) {
        StringBuilder sb = new StringBuilder();
        for (Cliente c : s.listarClientes()) {
            sb.append("<option value=\"").append(c.getId()).append("\">").append(esc(c.getNome())).append("</option>");
        }
        return sb.toString();
    }

    private static String optionsProdutos(LojaService s, boolean vazio) {
        StringBuilder sb = new StringBuilder();
        if (vazio) {
            sb.append("<option value=\"\">— nenhum —</option>");
        }
        for (Produto p : s.listarProdutos()) {
            sb.append("<option value=\"").append(p.getId()).append("\">").append(esc(p.getNome()))
                    .append(" — ").append(moeda(p.precoFinal())).append(" (").append(p.getEstoque().getQuantidade())
                    .append(" un.)</option>");
        }
        return sb.toString();
    }

    private static <E extends Enum<E>> String optionsEnum(E[] valores) {
        StringBuilder sb = new StringBuilder();
        for (E v : valores) {
            sb.append("<option value=\"").append(v.name()).append("\">").append(esc(v.name())).append("</option>");
        }
        return sb.toString();
    }

    static String layout(String titulo, String ativo, Map<String, String> q, String corpo) {
        String[][] menu = {
                {"/", "Início"}, {"/produtos", "Produtos"}, {"/clientes", "Clientes"}, {"/pedidos", "Pedidos"},
                {"/cupons", "Cupons"}, {"/avaliacoes", "Avaliações"}, {"/catalogo", "Catálogo"}
        };
        StringBuilder nav = new StringBuilder();
        for (String[] item : menu) {
            nav.append("<a href=\"").append(item[0]).append("\"")
                    .append(item[0].equals(ativo) ? " class=\"ativo\"" : "").append(">").append(item[1]).append("</a>");
        }
        String flash = "";
        if (q != null && q.containsKey("msg")) {
            String classe = "erro".equals(q.get("tipo")) ? "flash erro" : "flash ok";
            flash = "<div class=\"" + classe + "\">" + esc(q.get("msg")) + "</div>";
        }
        return "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + esc(titulo) + " · Loja Virtual</title><style>" + CSS + "</style></head><body>"
                + "<header><div class=\"wrap\"><strong class=\"marca\">Loja Virtual</strong><nav>" + nav
                + "</nav></div></header><main class=\"wrap\">" + flash + "<h1>" + esc(titulo) + "</h1>" + corpo
                + "</main></body></html>";
    }

    // ------------------------------------------------------------------ páginas

    static String inicio(LojaService s, Map<String, String> q) {
        BigDecimal receita = BigDecimal.ZERO;
        Map<Pedido.Status, Integer> porStatus = new EnumMap<>(Pedido.Status.class);
        for (Pedido p : s.listarPedidos()) {
            porStatus.merge(p.getStatus(), 1, Integer::sum);
            if (p.foiPago()) {
                receita = receita.add(p.total());
            }
        }
        StringBuilder b = new StringBuilder("<div class=\"cards\">");
        b.append(card("Produtos", String.valueOf(s.listarProdutos().size())));
        b.append(card("Clientes", String.valueOf(s.listarClientes().size())));
        b.append(card("Pedidos", String.valueOf(s.listarPedidos().size())));
        b.append(card("Receita (pedidos pagos)", moeda(receita)));
        b.append("</div><h2>Pedidos por status</h2><p>");
        for (Pedido.Status st : Pedido.Status.values()) {
            b.append(seloStatus(st)).append(" ").append(porStatus.getOrDefault(st, 0)).append(" &nbsp; ");
        }
        b.append("</p><h2>Estoque baixo</h2>");
        StringBuilder baixo = new StringBuilder();
        for (Produto p : s.listarProdutos()) {
            if (p.getEstoque().abaixoDoMinimo()) {
                baixo.append("<tr><td>").append(esc(p.getNome())).append("</td><td>")
                        .append(p.getEstoque().getQuantidade()).append("</td><td>")
                        .append(esc(p.getFornecedor().getNome())).append("</td><td>")
                        .append(p.getFornecedor().estimarReabastecimento(java.time.LocalDate.now()).format(DATA))
                        .append("</td></tr>");
            }
        }
        if (baixo.length() == 0) {
            b.append("<p class=\"mudo\">Nenhum produto abaixo do estoque mínimo.</p>");
        } else {
            b.append("<table><thead><tr><th>Produto</th><th>Qtd.</th><th>Fornecedor</th><th>Reposição estimada</th></tr></thead><tbody>")
                    .append(baixo).append("</tbody></table>");
        }
        return layout("Painel", "/", q, b.toString());
    }

    private static String card(String rotulo, String valor) {
        return "<div class=\"card\"><span>" + esc(rotulo) + "</span><strong>" + esc(valor) + "</strong></div>";
    }

    static String produtos(LojaService s, Map<String, String> q) {
        StringBuilder b = new StringBuilder("<table><thead><tr><th>#</th><th>Produto</th><th>Categoria</th>"
                + "<th>Preço</th><th>Estoque</th><th>Nota</th><th>Repor</th></tr></thead><tbody>");
        for (Produto p : s.listarProdutos()) {
            b.append("<tr><td>").append(p.getId()).append("</td><td><strong>").append(esc(p.getNome()))
                    .append("</strong><br><span class=\"mudo\">").append(esc(p.getDescricao())).append("</span></td><td>")
                    .append(esc(p.getCategoria().resumo())).append("</td><td>");
            if (p.getCategoria().temDesconto()) {
                b.append("<s class=\"mudo\">").append(moeda(p.getPreco())).append("</s><br>");
            }
            b.append(moeda(p.precoFinal())).append("</td><td>").append(esc(p.getEstoque())).append("</td><td>")
                    .append(p.getAvaliacoes().isEmpty() ? "—" : String.format("%.1f ★", p.mediaNotas()))
                    .append("</td><td><form method=\"post\" action=\"/produtos/estoque\" class=\"linha\">"
                            + "<input type=\"hidden\" name=\"id\" value=\"").append(p.getId())
                    .append("\"><input type=\"number\" name=\"qtd\" value=\"5\" min=\"1\" style=\"width:4.5em\">"
                            + "<button>+</button></form></td></tr>");
        }
        b.append("</tbody></table><h2>Novo produto</h2><form method=\"post\" action=\"/produtos\" class=\"grade\">"
                + "<label>Nome<input name=\"nome\" required></label>"
                + "<label>Descrição<input name=\"descricao\"></label>"
                + "<label>Preço (R$)<input name=\"preco\" required placeholder=\"99,90\"></label>"
                + "<label>Peso (kg)<input name=\"peso\" required placeholder=\"0,5\"></label>"
                + "<label>Categoria<select name=\"categoria\">");
        for (Categoria c : s.listarCategorias()) {
            b.append("<option value=\"").append(c.getId()).append("\">").append(esc(c.getNome())).append("</option>");
        }
        b.append("</select></label><label>Fornecedor<select name=\"fornecedor\">");
        for (Fornecedor f : s.listarFornecedores()) {
            b.append("<option value=\"").append(f.getId()).append("\">").append(esc(f.getNome())).append("</option>");
        }
        b.append("</select></label><label>Estoque inicial<input type=\"number\" name=\"estoque\" value=\"10\" min=\"0\"></label>"
                + "<div><button>Cadastrar produto</button></div></form>");
        return layout("Produtos", "/produtos", q, b.toString());
    }

    static String clientes(LojaService s, Map<String, String> q) {
        StringBuilder b = new StringBuilder("<table><thead><tr><th>#</th><th>Cliente</th><th>CPF</th>"
                + "<th>Fidelidade</th><th>Endereço</th></tr></thead><tbody>");
        for (Cliente c : s.listarClientes()) {
            String nivel = switch (c.getNivel()) {
                case OURO -> "amarelo";
                case PRATA -> "cinza";
                case BRONZE -> "laranja";
            };
            b.append("<tr><td>").append(c.getId()).append("</td><td><strong>").append(esc(c.getNome()))
                    .append("</strong><br><span class=\"mudo\">").append(esc(c.getEmail())).append("</span></td><td>")
                    .append(esc(c.getCpf())).append("</td><td>").append(selo(c.getNivel().name(), nivel)).append(" ")
                    .append(c.getPontos()).append(" pts</td><td>").append(esc(c.enderecoPrincipal())).append("</td></tr>");
        }
        b.append("</tbody></table><h2>Novo cliente</h2><form method=\"post\" action=\"/clientes\" class=\"grade\">"
                + "<label>Nome<input name=\"nome\" required></label>"
                + "<label>E-mail<input type=\"email\" name=\"email\" required></label>"
                + "<label>CPF<input name=\"cpf\" required placeholder=\"000.000.000-00\"></label>"
                + "<label>Rua<input name=\"rua\" required></label>"
                + "<label>Número<input name=\"numero\" required></label>"
                + "<label>Bairro<input name=\"bairro\" required></label>"
                + "<label>Cidade<input name=\"cidade\" required></label>"
                + "<label>UF<input name=\"estado\" maxlength=\"2\" required placeholder=\"MG\"></label>"
                + "<label>CEP<input name=\"cep\" required placeholder=\"00000-000\"></label>"
                + "<div><button>Cadastrar cliente</button></div></form>");
        return layout("Clientes", "/clientes", q, b.toString());
    }

    static String pedidos(LojaService s, Map<String, String> q) {
        StringBuilder b = new StringBuilder("<table><thead><tr><th>Pedido</th><th>Cliente</th><th>Itens</th>"
                + "<th>Total</th><th>Status</th><th>Criado em</th></tr></thead><tbody>");
        for (Pedido p : s.listarPedidos()) {
            b.append("<tr><td><a href=\"/pedido?id=").append(p.getId()).append("\">#").append(p.getId())
                    .append("</a></td><td>").append(esc(p.getCliente().getNome())).append("</td><td>")
                    .append(p.getItens().size()).append("</td><td>").append(moeda(p.total())).append("</td><td>")
                    .append(seloStatus(p.getStatus())).append("</td><td>").append(p.getCriadoEm().format(DATA_HORA))
                    .append("</td></tr>");
        }
        b.append("</tbody></table><h2>Novo pedido</h2><form method=\"post\" action=\"/pedidos\" class=\"grade\">"
                + "<label>Cliente<select name=\"cliente\">").append(optionsClientes(s)).append("</select></label>");
        for (int i = 1; i <= 3; i++) {
            b.append("<label>Produto ").append(i).append("<select name=\"produto").append(i).append("\">")
                    .append(optionsProdutos(s, i > 1)).append("</select></label>")
                    .append("<label>Quantidade<input type=\"number\" name=\"qtd").append(i)
                    .append("\" value=\"1\" min=\"1\"></label>");
        }
        b.append("<label>Cupom (opcional)<input name=\"cupom\" placeholder=\"BEMVINDO10\"></label>"
                + "<label>Entrega<select name=\"modalidade\">").append(optionsEnum(Frete.Modalidade.values()))
                .append("</select></label><div><button>Criar pedido</button></div></form>");
        return layout("Pedidos", "/pedidos", q, b.toString());
    }

    static String pedido(Pedido p, Map<String, String> q) {
        StringBuilder b = new StringBuilder();
        b.append("<p>").append(seloStatus(p.getStatus())).append(" &nbsp; Cliente: <strong>")
                .append(esc(p.getCliente().getNome())).append("</strong> (").append(p.getCliente().getNivel())
                .append(")<br>Entrega em: ").append(esc(p.getEnderecoEntrega())).append("<br>Modalidade: ")
                .append(p.getModalidade()).append(" — prazo estimado: ").append(p.prazoEstimadoDias())
                .append(" dia(s)</p><table><thead><tr><th>Produto</th><th>Qtd.</th><th>Unitário</th><th>Subtotal</th></tr></thead><tbody>");
        for (ItemPedido i : p.getItens()) {
            b.append("<tr><td>").append(esc(i.getProduto().getNome())).append("</td><td>").append(i.getQuantidade())
                    .append("</td><td>").append(moeda(i.getPrecoUnitario())).append("</td><td>")
                    .append(moeda(i.subtotal())).append("</td></tr>");
        }
        b.append("</tbody></table><table class=\"resumo\"><tr><td>Subtotal</td><td>").append(moeda(p.subtotal()))
                .append("</td></tr><tr><td>Desconto").append(p.getCupom() == null ? "" : " (" + esc(p.getCupom().getCodigo()) + ")")
                .append("</td><td>− ").append(moeda(p.desconto())).append("</td></tr><tr><td>Frete (")
                .append(p.pesoTotal().stripTrailingZeros().toPlainString()).append(" kg)</td><td>")
                .append(moeda(p.valorFrete())).append("</td></tr><tr class=\"total\"><td>Total</td><td>")
                .append(moeda(p.total())).append("</td></tr></table>");
        if (p.getPagamento() != null) {
            b.append("<p>Pagamento: <strong>").append(p.getPagamento().getMetodo()).append("</strong> em ")
                    .append(p.getPagamento().getParcelas()).append("x de ").append(moeda(p.getPagamento().valorParcela()))
                    .append(" — ").append(p.getPagamento().getStatus()).append("</p>");
        }
        b.append("<div class=\"acoes\">");
        if (p.getStatus() == Pedido.Status.CRIADO) {
            b.append("<form method=\"post\" action=\"/pedido/pagar\" class=\"linha\"><input type=\"hidden\" name=\"id\" value=\"")
                    .append(p.getId()).append("\"><select name=\"metodo\">").append(optionsEnum(Pagamento.Metodo.values()))
                    .append("</select><input type=\"number\" name=\"parcelas\" value=\"1\" min=\"1\" max=\"12\" style=\"width:4.5em\">"
                            + "<button>Pagar</button></form>");
        }
        b.append(botao(p, "enviar", "Marcar como enviado", p.getStatus() == Pedido.Status.PAGO))
                .append(botao(p, "entregar", "Marcar como entregue", p.getStatus() == Pedido.Status.ENVIADO))
                .append(botao(p, "cancelar", "Cancelar pedido", p.podeCancelar()))
                .append("</div><p><a href=\"/pedidos\">← Voltar aos pedidos</a></p>");
        return layout("Pedido #" + p.getId(), "/pedidos", q, b.toString());
    }

    private static String botao(Pedido p, String acao, String rotulo, boolean visivel) {
        if (!visivel) {
            return "";
        }
        String classe = "cancelar".equals(acao) ? " class=\"perigo\"" : "";
        return "<form method=\"post\" action=\"/pedido/" + acao + "\"><input type=\"hidden\" name=\"id\" value=\""
                + p.getId() + "\"><button" + classe + ">" + rotulo + "</button></form>";
    }

    static String cupons(LojaService s, Map<String, String> q) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        StringBuilder b = new StringBuilder("<table><thead><tr><th>Código</th><th>Desconto</th><th>Validade</th>"
                + "<th>Mínimo</th><th>Usos</th><th>Nível</th><th>Situação</th></tr></thead><tbody>");
        for (Cupom c : s.listarCupons()) {
            String situacao = !c.isAtivo() ? selo("INATIVO", "cinza")
                    : c.expirado(hoje) ? selo("EXPIRADO", "vermelho") : selo("VÁLIDO", "verde");
            b.append("<tr><td><strong>").append(esc(c.getCodigo())).append("</strong></td><td>")
                    .append(esc(c.descricao().substring(c.getCodigo().length() + 3))).append("</td><td>")
                    .append(c.getValidade() == null ? "sem validade" : c.getValidade().format(DATA)).append("</td><td>")
                    .append(c.getValorMinimo() == null ? "—" : moeda(c.getValorMinimo())).append("</td><td>")
                    .append(c.getUsos()).append(c.getUsoMaximo() > 0 ? " / " + c.getUsoMaximo() : "").append("</td><td>")
                    .append(c.getNivelExigido() == null ? "todos" : c.getNivelExigido() + "+").append("</td><td>")
                    .append(situacao).append("</td></tr>");
        }
        b.append("</tbody></table><h2>Novo cupom</h2><form method=\"post\" action=\"/cupons\" class=\"grade\">"
                + "<label>Código<input name=\"codigo\" required></label>"
                + "<label>Tipo<select name=\"tipo\">").append(optionsEnum(Cupom.Tipo.values())).append("</select></label>"
                + "<label>Valor (% ou R$)<input name=\"valor\" required placeholder=\"10\"></label>"
                + "<label>Validade (opcional)<input type=\"date\" name=\"validade\"></label>"
                + "<label>Pedido mínimo (R$)<input name=\"minimo\" placeholder=\"0\"></label>"
                + "<label>Limite de usos (0 = ilimitado)<input type=\"number\" name=\"usoMax\" value=\"0\" min=\"0\"></label>"
                + "<label>Nível mínimo<select name=\"nivel\"><option value=\"\">Todos</option>")
                .append(optionsEnum(Cliente.Nivel.values())).append("</select></label>"
                        + "<div><button>Cadastrar cupom</button></div></form>");
        return layout("Cupons", "/cupons", q, b.toString());
    }

    static String avaliacoes(LojaService s, Map<String, String> q) {
        StringBuilder b = new StringBuilder();
        if (s.listarAvaliacoes().isEmpty()) {
            b.append("<p class=\"mudo\">Ainda não há avaliações.</p>");
        } else {
            b.append("<table><thead><tr><th>Produto</th><th>Nota</th><th>Cliente</th><th>Comentário</th><th>Data</th></tr></thead><tbody>");
            for (Avaliacao a : s.listarAvaliacoes()) {
                b.append("<tr><td>").append(esc(a.getProduto().getNome())).append("</td><td class=\"estrelas\">")
                        .append(esc(a.estrelas())).append("</td><td>").append(esc(a.getCliente().getNome()))
                        .append("</td><td>").append(esc(a.getComentario())).append("</td><td>")
                        .append(a.getData().format(DATA)).append("</td></tr>");
            }
            b.append("</tbody></table>");
        }
        b.append("<h2>Nova avaliação</h2><p class=\"mudo\">Apenas clientes que já pagaram um pedido com o produto podem avaliá-lo.</p>"
                + "<form method=\"post\" action=\"/avaliacoes\" class=\"grade\"><label>Cliente<select name=\"cliente\">")
                .append(optionsClientes(s)).append("</select></label><label>Produto<select name=\"produto\">")
                .append(optionsProdutos(s, false)).append("</select></label>"
                        + "<label>Nota<select name=\"nota\"><option>5</option><option>4</option><option>3</option>"
                        + "<option>2</option><option>1</option></select></label>"
                        + "<label>Comentário<input name=\"comentario\" maxlength=\"500\"></label>"
                        + "<div><button>Enviar avaliação</button></div></form>");
        return layout("Avaliações", "/avaliacoes", q, b.toString());
    }

    static String catalogo(LojaService s, Map<String, String> q) {
        StringBuilder b = new StringBuilder("<h2>Categorias</h2><table><thead><tr><th>Categoria</th><th>Descrição</th>"
                + "<th>Desconto</th></tr></thead><tbody>");
        for (Categoria c : s.listarCategorias()) {
            b.append("<tr><td>").append(esc(c.getNome())).append("</td><td>").append(esc(c.getDescricao()))
                    .append("</td><td>").append(c.temDesconto() ? c.getPercentualDesconto().stripTrailingZeros().toPlainString() + "%" : "—")
                    .append("</td></tr>");
        }
        b.append("</tbody></table><h2>Fornecedores</h2><table><thead><tr><th>Fornecedor</th><th>CNPJ</th>"
                + "<th>E-mail</th><th>Prazo</th><th>Situação</th></tr></thead><tbody>");
        for (Fornecedor f : s.listarFornecedores()) {
            b.append("<tr><td>").append(esc(f.getNome())).append("</td><td>").append(esc(f.getCnpj())).append("</td><td>")
                    .append(esc(f.getEmail())).append("</td><td>").append(f.getPrazoEntregaDias()).append(" dias</td><td>")
                    .append(f.isAtivo() ? selo("ATIVO", "verde") : selo("INATIVO", "cinza")).append("</td></tr>");
        }
        b.append("</tbody></table>");
        return layout("Catálogo", "/catalogo", q, b.toString());
    }

    static String naoEncontrada(Map<String, String> q) {
        return layout("Página não encontrada", "", q, "<p>O endereço solicitado não existe. <a href=\"/\">Voltar ao início</a></p>");
    }

    // ------------------------------------------------------------------ estilo

    private static final String CSS = """
            :root{--bg:#f6f7f9;--fg:#1d2330;--mudo:#6b7385;--borda:#dfe3ea;--cor:#2457d6;--card:#fff}
            @media (prefers-color-scheme:dark){:root{--bg:#12151c;--fg:#e8ebf2;--mudo:#9aa3b6;--borda:#2a3040;--cor:#6d9bff;--card:#1a1f2b}}
            *{box-sizing:border-box}body{margin:0;font:15px/1.5 system-ui,sans-serif;background:var(--bg);color:var(--fg)}
            .wrap{max-width:1040px;margin:0 auto;padding:0 20px}
            header{background:var(--card);border-bottom:1px solid var(--borda)}
            header .wrap{display:flex;gap:24px;align-items:center;flex-wrap:wrap;padding-top:12px;padding-bottom:12px}
            .marca{font-size:18px}nav{display:flex;gap:4px;flex-wrap:wrap}
            nav a{padding:6px 12px;border-radius:6px;color:var(--fg);text-decoration:none}
            nav a:hover{background:var(--bg)}nav a.ativo{background:var(--cor);color:#fff}
            main{padding-top:8px;padding-bottom:48px}h1{font-size:26px;margin:24px 0 12px}h2{font-size:18px;margin:32px 0 10px}
            table{width:100%;border-collapse:collapse;background:var(--card);border:1px solid var(--borda);border-radius:8px;overflow:hidden}
            th,td{padding:9px 12px;text-align:left;border-bottom:1px solid var(--borda);vertical-align:top}
            th{font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:var(--mudo);background:var(--bg)}
            tr:last-child td{border-bottom:0}a{color:var(--cor)}.mudo{color:var(--mudo);font-size:13px}
            .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px}
            .card{background:var(--card);border:1px solid var(--borda);border-radius:8px;padding:14px 16px}
            .card span{display:block;color:var(--mudo);font-size:13px}.card strong{font-size:22px}
            .selo{display:inline-block;padding:1px 9px;border-radius:99px;font-size:12px;font-weight:600;background:var(--borda)}
            .azul{background:#dbe7ff;color:#173b93}.verde{background:#d5f2dd;color:#14602b}.vermelho{background:#fbdcdc;color:#8c1d1d}
            .amarelo{background:#fff0c2;color:#6d5300}.laranja{background:#fde1cc;color:#8a3d00}.cinza{background:var(--borda);color:var(--fg)}
            form.grade{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;background:var(--card);
            border:1px solid var(--borda);border-radius:8px;padding:16px;align-items:end}
            label{display:flex;flex-direction:column;gap:4px;font-size:13px;color:var(--mudo)}
            input,select{font:inherit;padding:7px 9px;border:1px solid var(--borda);border-radius:6px;background:var(--bg);color:var(--fg)}
            button{font:inherit;padding:8px 16px;border:0;border-radius:6px;background:var(--cor);color:#fff;cursor:pointer}
            button.perigo{background:#c0392b}form.linha{display:flex;gap:6px}.acoes{display:flex;gap:10px;flex-wrap:wrap;margin:16px 0}
            .flash{margin-top:16px;padding:10px 14px;border-radius:8px}.flash.ok{background:#d5f2dd;color:#14602b}
            .flash.erro{background:#fbdcdc;color:#8c1d1d}table.resumo{width:320px;margin:16px 0 16px auto}
            table.resumo td:last-child{text-align:right}.total td{font-weight:700;font-size:17px}.estrelas{color:#d89b00;white-space:nowrap}
            """;
}
