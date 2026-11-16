# Loja Virtual (Java)

Aplicação web de loja virtual em Java puro: servidor HTTP embutido no JDK (`com.sun.net.httpserver`),
páginas HTML renderizadas no servidor e dados em memória (com dados de exemplo). **Não precisa de Maven,
Gradle nem de nenhuma dependência externa.**

## Como executar

Requisito: JDK 17 ou superior (testado com o 21).

**Linux / macOS / WSL**

```bash
./run.sh          # sobe em http://localhost:8080
./run.sh 9090     # outra porta
```

**Windows (PowerShell ou CMD)**: use o `run.bat`, não o `run.sh`.

```powershell
.\run.bat          # sobe em http://localhost:8080
.\run.bat 9090     # outra porta
```

Manualmente no PowerShell, a partir da pasta do projeto:

```powershell
mkdir out
javac -encoding UTF-8 -d out src\loja\dominio\*.java src\loja\servico\*.java src\loja\web\*.java
java -cp out loja.web.Servidor
```

## O que a aplicação faz

| Página | Recursos |
|---|---|
| `/` | painel com totais, receita, pedidos por status e produtos com estoque baixo |
| `/produtos` | listagem, cadastro e reabastecimento de estoque |
| `/clientes` | listagem e cadastro (valida e-mail, CPF e UF) |
| `/pedidos`, `/pedido?id=` | criação de pedido (até 3 produtos, cupom, modalidade de entrega), pagamento, envio, entrega e cancelamento |
| `/cupons` | listagem e cadastro de cupons |
| `/avaliacoes` | avaliações (só quem pagou um pedido com o produto pode avaliar) |
| `/catalogo` | categorias e fornecedores |
| `/api/produtos` | JSON com os produtos |

## Camadas

```
src/loja/dominio   12 classes de domínio (regras de negócio)
src/loja/servico   LojaService: casos de uso e dados em memória
src/loja/web       Servidor (rotas HTTP) e Paginas (HTML)
```

## Classes de domínio

Todas têm atributos e métodos com regra de negócio.

| Classe | Atributos principais | Métodos principais |
|---|---|---|
| `Endereco` | rua, número, bairro, cidade, estado, cep | `cepValido`, `regiao`, `enderecoCompleto` |
| `Cliente` | id, nome, e-mail, cpf, endereços, pontos | `cpfValido`, `emailValido`, `getNivel`, `adicionarPontos` |
| `Categoria` | id, nome, descrição, percentualDesconto | `aplicarDesconto`, `temDesconto` |
| `Fornecedor` | id, nome, cnpj, prazoEntregaDias, ativo | `cnpjValido`, `estimarReabastecimento`, `desativar` |
| `Estoque` | quantidade, mínimo | `retirar`, `adicionar`, `abaixoDoMinimo` |
| `Produto` | id, nome, preço, peso, categoria, fornecedor, estoque, avaliações | `precoFinal`, `disponivel`, `mediaNotas`, `alterarPreco` |
| `ItemPedido` | produto, quantidade, precoUnitario | `subtotal`, `pesoTotal`, `somar` |
| `Pedido` | id, cliente, itens, status, cupom, pagamento | `adicionarItem`, `total`, `pagar`, `cancelar` |
| `Pagamento` | id, método, valor, parcelas, status | `processar`, `valorParcela`, `estornar` |
| `Cupom` | código, tipo, valor, validade, mínimo, usos, nível | **`calcularDesconto`**, `registrarUso`, `expirado` |
| `Frete` | tarifas por região, adicional por kg, limite de frete grátis | **`calcular`**, `prazoEstimadoDias` |
| `Avaliacao` | id, cliente, produto, nota, comentário | `validar`, `estrelas` |

## Observações

- Os dados ficam só em memória: reiniciar o servidor volta ao estado inicial de exemplo.
- Cupons de exemplo: `BEMVINDO10`, `FIXO30` (mínimo R$ 200), `OURO15` (só nível OURO) e `VERAO` (expirado).
- Regras de frete: tarifa por região, +R$ 3,50/kg acima de 5 kg, expresso x1,8, retirada grátis,
  cliente OURO -20% e frete grátis a partir de R$ 300.
