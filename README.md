# 🛒 Projeto PDV (Ponto de Venda)

Sistema completo de Ponto de Venda (PDV) desenvolvido em **Java puro** com persistência em **SQLite**, arquitetura modular em camadas, terminal interativo com suporte a sessões contextuais e padrão de comandos (*Command Pattern*).

---

## 📌 Principais Funcionalidades

- **Autenticação e Controle de Acesso RBAC (Role-Based Access Control)**:
  - Exigência estrita de autenticação via `/login` com prompt interativo seguro.
  - Criptografia padrão industrial: SHA-256 com Salt aleatório de 16 bytes e comparação constante contra *timing attacks*.
  - Perfis base (`ADMIN`, `GERENTE`, `VENDEDOR`, `CAIXA`) com permissões padrão integradas.
  - Árvore hierárquica de permissões com herança mãe/filhas e concessão/revogação granular por operador.
  - Mensagens descritivas detalhadas de permissão negada informando o código da permissão e sua finalidade.
- **Catálogo de Produtos**: Cadastro, edição, exclusão e consulta flexível com busca por código de barras, ID ou busca parcial por curinga (`%`).
- **Gestão de Clientes**: Cadastro com validação de CPF e data de nascimento no formato brasileiro (`dd/MM/yyyy`).
- **Módulo Avançado de Orçamentos**:
  - Sessão interativa dedicada com prompt contextual (`[Orçamento #N - Nome] >`).
  - Máquina de estados com regras estritas: `ABERTO`, `CONFIRMADO`, `CANCELADO`, `FATURANDO`, `FINALIZADO`.
  - Snapshot de preços e catálogo: itens retêm o preço praticado e dados no momento da adição, mesmo que o catálogo de produtos seja alterado posteriormente.
  - Suporte completo a descontos (percentual `%` ou valor em `R$`) com validação de permissão de aprovação.
  - Duplicação inteligente de orçamentos com detecção de divergências cadastrais.
  - Recuperação de orçamentos descartados/cancelados.
  - Emissão de resumo formatado estilo cupom de venda.
- **Formatação e Padrões Brasileiros**:
  - Valores monetários com precisão absoluta via `BigDecimal` e persistência em centavos inteiros (`INTEGER`).
  - Datas e horários 100% padronizados no formato brasileiro (`dd/MM/yyyy` e `dd/MM/yyyy HH:mm:ss`).

---

## 📁 Estrutura do Projeto

```text
projetopdv/
├── .vscode/
│   ├── settings.json               # Configurações de classpath do Java
│   └── launch.json                 # Perfis de execução no VS Code / AntiGravity
├── bin/                            # Binários compilados (.class)
├── lib/
│   └── sqlite-jdbc-*.jar           # Driver JDBC do SQLite
├── src/
│   └── projetopdv/
│       ├── cliente/                # Entidade Cliente e ClienteDAO
│       ├── dados/                  # Inicialização e conexão SQLite (BancoDeDados)
│       ├── orcamento/              # Entidades Orcamento, ItemOrcamento e OrcamentoDAO
│       ├── produto/                # Entidade Produto e ProdutoDAO
│       ├── seguranca/              # CriptografiaUtil (SHA-256 + Salt)
│       ├── ui/                     # Painel CLI e controle de SessaoOrcamento
│       │   └── comandos/           # Padrão Command do Menu Principal
│       │       └── orcamento/      # Sub-comandos contextuais da Sessão de Orçamento
│       ├── usuario/                # Entidade Usuario, PerfilUsuario, Permissao, UsuarioDAO
│       └── venda/                  # Modelos de Venda e ItemVenda
├── projetopdv.db                   # Banco de dados SQLite local
└── README.md                       # Documentação do projeto
```

---

## ⌨️ Guia de Comandos da CLI

### 🌐 Comandos Globais (Menu Principal)

| Comando | Descrição | Permissão Necessária | Exemplo de Uso |
| :--- | :--- | :--- | :--- |
| `/login` | Autentica um operador no sistema solicitando usuário e senha. | Nenhuma (Público) | `/login` |
| `/logout` | Encerra a sessão do usuário autenticado no sistema. | Autenticado | `/logout` |
| `/help` ou `/ajuda` | Exibe os comandos disponíveis ou detalhes de um comando específico. Digite `++` para ver todos. | Nenhuma (Público) | `/help`<br>`/help /usuario` |
| `/cadastrar_usuario` | Cadastra um novo operador com perfil base (`ADMIN`, `GERENTE`, `VENDEDOR`, `CAIXA`). | `USUARIO_CADASTRAR` | `/cadastrar_usuario` |
| `/usuario` | Gerencia operadores, altera perfil, redefine senha, lista e concede/revoga permissões granulares. | `USUARIO_CONSULTAR`<br>`USUARIO_EDITAR`<br>`USUARIO_ALTERAR_PERMISSOES`<br>`USUARIO_ALTERAR_SENHA` | `/usuario 2`<br>`/usuario 2 permissoes`<br>`/usuario 2 conceder ORCAMENTO_APROVAR_DESCONTO`<br>`/usuario 2 revogar PRODUTO_EDITAR` |
| `/cadastrar_produto` | Inicia o cadastro guiado de um novo produto (Nome, Cód. Barras, Preço). | `PRODUTO_CADASTRAR` | `/cadastrar_produto` |
| `/consultar_produtos` | Lista todos os produtos cadastrados no sistema. | `PRODUTO_CONSULTAR` | `/consultar_produtos` |
| `/produto` | Exibe ou altera propriedades de um produto via ID. | `PRODUTO_CONSULTAR`<br>`PRODUTO_EDITAR`<br>`PRODUTO_EXCLUIR` | `/produto 1`<br>`/produto 1 set_preco 25.90`<br>`/produto 1 deletar` |
| `/cadastrar_cliente` | Inicia o cadastro guiado de um novo cliente (Nome, Data Nasc. `DD/MM/AAAA`, CPF). | `CLIENTE_CADASTRAR` | `/cadastrar_cliente` |
| `/consultar_clientes` | Lista todos os clientes cadastrados. | `CLIENTE_CONSULTAR` | `/consultar_clientes` |
| `/cliente` | Exibe ou altera propriedades de um cliente via ID. | `CLIENTE_CONSULTAR`<br>`CLIENTE_EDITAR`<br>`CLIENTE_EXCLUIR` | `/cliente 1`<br>`/cliente 1 set_nome "Maria Silva"`<br>`/cliente 1 deletar` |
| `/novo_orcamento` | Cria um novo orçamento aberto e entra automaticamente em sua sessão interativa. | `ORCAMENTO_CRIAR` | `/novo_orcamento`<br>`/novo_orcamento "Mesa 01"` |
| `/abrir` | Lista orçamentos disponíveis para edição ou abre diretamente por ID/Nome. | `ORCAMENTO_CONSULTAR` | `/abrir`<br>`/abrir 3` |
| `/recuperar` | Lista ou reabre diretamente orçamentos cancelados para o status `ABERTO`. | `ORCAMENTO_CONSULTAR` | `/recuperar`<br>`/recuperar 2` |
| `/orcamentos` | Lista orçamentos com filtros ou exibe detalhes em modo somente-leitura. | `ORCAMENTO_CONSULTAR` | `/orcamentos`<br>`/orcamentos CONFIRMADO` |
| `/duplicar` | Clona qualquer orçamento existente gerando um novo orçamento aberto. | `ORCAMENTO_DUPLICAR` | `/duplicar`<br>`/duplicar 1` |
| `/descartar` | Cancela ou descarta um orçamento do sistema. | `ORCAMENTO_CANCELAR` | `/descartar 1` |
| `/resetar_banco` | Limpa registros e zera contadores de ID de uma tabela ou de todo o banco de dados. Exige confirmação explícita e reautenticação de senha. | Exclusivo `ADMIN` | `/resetar_banco All`<br>`/resetar_banco produto`<br>`/resetar_banco cliente`<br>`/resetar_banco orcamento`<br>`/resetar_banco usuario` |
| `/sair` | Encerra a aplicação. | Nenhuma (Público) | `/sair` |

---

### 📝 Comandos da Sessão de Orçamento (`[Orçamento #N] >`)

Ao criar ou abrir um orçamento, o terminal entra no modo de sessão dedicada. Os seguintes comandos ficam disponíveis:

| Comando | Descrição | Exemplo de Uso |
| :--- | :--- | :--- |
| `/add` | Adiciona um item ao orçamento ativo por ID, Código de Barras ou busca flexível por termo com `%`. | `/add 1 2`<br>`/add 7891001 3`<br>`/add %shampoo%200 2` |
| `/remove_item` | Remove um item do orçamento ativo pelo seu número sequencial. | `/remove_item 1` |
| `/edit_item` | Altera quantidade ou aplica descontos/preços especiais no item. | `/edit_item 1 qtd 5`<br>`/edit_item 1 desc_pct 10`<br>`/edit_item 1 desc_val 5.00`<br>`/edit_item 1 valor 18.90` |
| `/renomear` | Define ou altera o nome de identificação do orçamento ativo. | `/renomear "Mesa Varanda 04"` |
| `/set_cliente` | Vincula um cliente ao orçamento por ID, CPF ou Nome. | `/set_cliente 1`<br>`/set_cliente "Maria Silva"` |
| `/remove_cliente` | Desvincula o cliente do orçamento ativo. | `/remove_cliente` |
| `/resumo` | Exibe o cupom detalhado do orçamento com subtotais, descontos e total líquido. | `/resumo` |
| `/confirmar` | Valida unicidade de nome, solicita aprovação caso haja descontos e confirma o orçamento para faturamento no caixa. | `/confirmar` |
| `/descartar` | Cancela o orçamento ativo (pode ser recuperado depois com `/recuperar`). | `/descartar` |
| `/sair` | Sai da sessão mantendo o orçamento salvo no estado atual e retorna ao Menu Principal. | `/sair` |

---

## 🚀 Como Executar

### 1. Pelo VS Code
1. Abra a pasta do projeto `projetopdv`.
2. Pressione `F5` ou acesse a aba **Run and Debug** (`Ctrl+Shift+D` / `Cmd+Shift+D`).
3. Selecione o perfil **"Executar Painel (PDV)"**.

### 2. Pelo Terminal

Na raiz do projeto (`projetopdv`):

```bash
# Compilar todas as classes Java
find src -name "*.java" | xargs javac -d bin -cp "lib/sqlite-jdbc-3.53.4.0.jar:src"

# Executar a aplicação
java -cp "bin:lib/sqlite-jdbc-3.53.4.0.jar" projetopdv.ui.Painel
```

---

## 🛠️ Tecnologias e Padrões Utilizados

- **Java SE 17+** (com recursos modernos como *Records*, *Text Blocks*, *Pattern Matching* e `java.time`).
- **SQLite JDBC** com chaves estrangeiras ativadas (`PRAGMA foreign_keys = ON`).
- **Arquitetura em Camadas** (*Package-by-Feature*).
- **Padrão DAO (Data Access Object)** com controle transacional (`commit`/`rollback`).
- **Padrão Command** para tratamento desacoplado de entradas do console.
- **Padrão State** para gerenciamento do ciclo de vida dos orçamentos.
