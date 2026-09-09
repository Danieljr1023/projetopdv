package projetopdv.ui;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import projetopdv.cliente.ClienteDAO;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.comandos.ComandoDuplicarOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.ui.comandos.ComandosPainel;
import projetopdv.ui.comandos.orcamento.ComandoAddItem;
import projetopdv.ui.comandos.orcamento.ComandoConfirmarOrcamento;
import projetopdv.ui.comandos.orcamento.ComandoDescartarOrcamento;
import projetopdv.ui.comandos.orcamento.ComandoEditItem;
import projetopdv.ui.comandos.orcamento.ComandoRemoveCliente;
import projetopdv.ui.comandos.orcamento.ComandoRemoveItem;
import projetopdv.ui.comandos.orcamento.ComandoRenomearOrcamento;
import projetopdv.ui.comandos.orcamento.ComandoResumoOrcamento;
import projetopdv.ui.comandos.orcamento.ComandoSairSessao;
import projetopdv.ui.comandos.orcamento.ComandoSetCliente;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;

public final class SessaoOrcamento {

    public static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Scanner entrada;
    private final OrcamentoDAO orcamentoDAO;
    private final ProdutoDAO produtoDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioDAO usuarioDAO;
    private final ComandosPainel comandosGerais;
    private final ComandosPainel comandosOrcamento;

    private Usuario usuarioLogado = null;

    private int idOrcamentoAtivo = 0;
    private List<Produto> opcoesProdutosPendentes = null;
    private int quantidadePendente = 1;
    private BigDecimal precoPendente = null;

    private List<Orcamento> opcoesOrcamentosParaAbrir = null;
    private List<Orcamento> opcoesOrcamentosParaRecuperar = null;
    private List<Orcamento> opcoesOrcamentosParaDescartar = null;

    private boolean ultimoComandoFoiHelp = false;

    public SessaoOrcamento(
            Scanner entrada,
            OrcamentoDAO orcamentoDAO,
            ProdutoDAO produtoDAO,
            ClienteDAO clienteDAO,
            UsuarioDAO usuarioDAO,
            ComandosPainel comandosGerais
    ) {
        this.entrada = entrada;
        this.orcamentoDAO = orcamentoDAO;
        this.produtoDAO = produtoDAO;
        this.clienteDAO = clienteDAO;
        this.usuarioDAO = usuarioDAO;
        this.comandosGerais = comandosGerais;
        this.comandosOrcamento = new ComandosPainel();

        registrarComandosOrcamento();
    }

    private void registrarComandosOrcamento() {
        comandosOrcamento.registrar(new ComandoAddItem(this));
        comandosOrcamento.registrar(new ComandoRemoveItem(this));
        comandosOrcamento.registrar(new ComandoEditItem(this));
        comandosOrcamento.registrar(new ComandoRenomearOrcamento(this));
        comandosOrcamento.registrar(new ComandoDuplicarOrcamento(this));
        comandosOrcamento.registrar(new ComandoSetCliente(this));
        comandosOrcamento.registrar(new ComandoRemoveCliente(this));
        comandosOrcamento.registrar(new ComandoResumoOrcamento(this));
        comandosOrcamento.registrar(new ComandoConfirmarOrcamento(this));
        comandosOrcamento.registrar(new ComandoDescartarOrcamento(this));
        comandosOrcamento.registrar(new ComandoSairSessao(this));
    }

    public boolean isAutenticado() {
        return usuarioLogado != null;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }

    public void deslogar() {
        if (temOrcamentoAtivo()) {
            sairOrcamento();
        }
        this.usuarioLogado = null;
    }

    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }

    /**
     * Lê uma senha do operador ocultando a digitação no terminal (sem eco de
     * caracteres). Se o ambiente não possuir um Console interativo disponível
     * (ex: testes automatizados ou pipes), utiliza a entrada padrão (Scanner).
     *
     * @param prompt Mensagem a ser exibida ao operador (ex: "Digite a senha: ")
     * @return A senha informada pelo operador em texto limpo
     */
    public String lerSenha(String prompt) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] senhaChars = console.readPassword("%s", prompt);
            if (senhaChars == null) {
                return "";
            }
            return new String(senhaChars);
        } else {
            System.out.print(prompt);
            System.out.flush();
            if (entrada.hasNextLine()) {
                return entrada.nextLine();
            }
            return "";
        }
    }

    public boolean validarPermissao(Permissao permissaoNecessaria) {
        if (!isAutenticado()) {
            System.out.println("Acesso negado: Você precisa fazer login antes de utilizar o sistema.");
            System.out.println("Digite /login para entrar ou /sair para encerrar.");
            return false;
        }

        if (permissaoNecessaria != null && !usuarioLogado.temPermissao(permissaoNecessaria)) {
            System.out.println("Acesso Negado: Você não possui permissão para executar esta ação.");
            System.out.println("Permissão necessária: " + permissaoNecessaria.name() + " (" + permissaoNecessaria.getDescricao() + ")");
            return false;
        }

        return true;
    }

    public boolean temPermissao(Permissao permissao) {
        if (usuarioLogado == null) {
            return false;
        }
        return usuarioLogado.temPermissao(permissao);
    }

    public boolean temOrcamentoAtivo() {
        return idOrcamentoAtivo > 0;
    }

    public int getIdOrcamentoAtivo() {
        return idOrcamentoAtivo;
    }

    public Orcamento getOrcamentoAtivo() {
        if (!temOrcamentoAtivo()) {
            return null;
        }
        return orcamentoDAO.buscarPorId(idOrcamentoAtivo);
    }

    public void entrarOrcamento(int idOrcamento) {
        this.idOrcamentoAtivo = idOrcamento;
        limparOpcoesPendentes();
        Orcamento orc = getOrcamentoAtivo();
        if (orc != null) {
            String nome;
            try {
                nome = orc.getNomeOrcamento();
            } catch (Exception e) {
                nome = "Sem Nome";
            }
            System.out.println(String.format("\n[SESSÃO ATIVA: Orçamento #%d - %s | Status: %s | Total: R$ %.2f]",
                    orc.getIdOrcamento(), nome, orc.getStatusOrcamento(), orc.getValorTotal()));
            System.out.println("Digite /help para ver os comandos disponíveis no orçamento.");
        }
    }

    public void sairOrcamento() {
        if (temOrcamentoAtivo()) {
            System.out.println(String.format("\n[SAINDO DA SESSÃO: Orçamento #%d mantido no estado atual]", idOrcamentoAtivo));
        }
        this.idOrcamentoAtivo = 0;
        limparOpcoesPendentes();
    }

    public String obterPrompt() {
        if (!isAutenticado()) {
            return "\n[PDV - Não Autenticado] > ";
        }

        String prefixoUsuario = String.format("[%s: %s]", usuarioLogado.getPerfil().name(), usuarioLogado.getLogin());

        if (!temOrcamentoAtivo()) {
            return String.format("\n%s > ", prefixoUsuario);
        }

        Orcamento orc = getOrcamentoAtivo();
        String nome;
        try {
            nome = (orc != null && !orc.getNomeOrcamento().isEmpty()) ? orc.getNomeOrcamento() : "Sem Nome";
        } catch (Exception e) {
            nome = "Sem Nome";
        }
        return String.format("\n%s [Orçamento #%d - %s] > ", prefixoUsuario, idOrcamentoAtivo, nome);
    }

    public boolean processarLinha(String linha) {
        String linhaLimpa = linha.trim();
        if (linhaLimpa.isEmpty()) {
            return true;
        }

        // 1. Verificar se é '++' logo após '/help'
        if (linhaLimpa.equals("++") && ultimoComandoFoiHelp) {
            ultimoComandoFoiHelp = false;
            System.out.println("\n=== TODOS OS COMANDOS DO SISTEMA ===");
            for (ComandoPainel c : comandosGerais.listar()) {
                System.out.println(String.format("  %-20s : %s", c.getNome(), c.getDescricao().lines().findFirst().orElse("")));
            }
            System.out.println("\n=== COMANDOS DO ORÇAMENTO ===");
            for (ComandoPainel c : comandosOrcamento.listar()) {
                System.out.println(String.format("  %-20s : %s", c.getNome(), c.getDescricao().lines().findFirst().orElse("")));
            }
            return true;
        }
        ultimoComandoFoiHelp = false;

        // 2. Verificar se há seleção pendente de produto
        if (temOpcoesProdutosPendentes()) {
            if (processarSelecaoOpcaoProduto(linhaLimpa)) {
                return true;
            }
        }

        // 3. Verificar se há seleção pendente para abrir orçamento
        if (temOpcoesAbrirPendentes()) {
            if (processarSelecaoOpcaoAbrir(linhaLimpa)) {
                return true;
            }
        }

        // 4. Verificar se há seleção pendente para recuperar orçamento
        if (temOpcoesRecuperarPendentes()) {
            if (processarSelecaoOpcaoRecuperar(linhaLimpa)) {
                return true;
            }
        }

        // 5. Verificar se há seleção pendente para descartar orçamento
        if (temOpcoesDescartarPendentes()) {
            if (processarSelecaoOpcaoDescartar(linhaLimpa)) {
                return true;
            }
        }

        // 6. Tratar comando
        String[] partes = separarArgumentos(linhaLimpa);
        String nomeComando = partes[0].toLowerCase();
        String[] argumentos = new String[partes.length - 1];
        System.arraycopy(partes, 1, argumentos, 0, partes.length - 1);

        // Se não estiver autenticado, apenas /login, /sair e /help são permitidos
        if (!isAutenticado()) {
            if (nomeComando.equals("/login")) {
                ComandoPainel cmdLogin = comandosGerais.buscar("/login");
                if (cmdLogin != null) {
                    return cmdLogin.executar(argumentos);
                }
            }
            if (nomeComando.equals("/sair")) {
                ComandoPainel cmdSair = comandosGerais.buscar("/sair");
                if (cmdSair != null) {
                    return cmdSair.executar(argumentos);
                }
            }
            if (nomeComando.equals("/help") || nomeComando.equals("/ajuda")) {
                System.out.println("\n==================================================");
                System.out.println("           SISTEMA PDV - ACESSO INICIAL           ");
                System.out.println("==================================================");
                System.out.println("Comandos disponíveis antes da autenticação:");
                System.out.println("  /login              Inicia login interativo (ou /login <usuário> <senha>)");
                System.out.println("  /sair               Encerra a aplicação");
                System.out.println("  /help               Exibe esta mensagem de ajuda");
                System.out.println("==================================================");
                return true;
            }
            System.out.println("Acesso negado: Você precisa fazer login antes de utilizar o sistema.");
            System.out.println("Digite /login para entrar ou /sair para encerrar.");
            return true;
        }

        if (nomeComando.equals("/help") || nomeComando.equals("/ajuda")) {
            if (argumentos.length == 0) {
                ultimoComandoFoiHelp = true;
                exibirAjudaContextual();
            } else {
                exibirAjudaDetalhada(argumentos[0]);
            }
            return true;
        }

        ultimoComandoFoiHelp = false;

        // Se está dentro de um orçamento, tenta primeiro nos comandos do orçamento
        if (temOrcamentoAtivo()) {
            ComandoPainel comandoOrc = comandosOrcamento.buscar(nomeComando);
            if (comandoOrc != null) {
                return comandoOrc.executar(argumentos);
            }
        }

        // Tenta nos comandos gerais
        ComandoPainel comandoGeral = comandosGerais.buscar(nomeComando);
        if (comandoGeral != null) {
            return comandoGeral.executar(argumentos);
        }

        System.out.println("Comando '" + nomeComando + "' não reconhecido. Digite /help para consultar os comandos.");
        return true;
    }

    public void exibirAjudaContextual() {
        if (temOrcamentoAtivo()) {
            System.out.println("\n=== COMANDOS DISPONÍVEIS NO ORÇAMENTO ATIVO ===");
            for (ComandoPainel c : comandosOrcamento.listar()) {
                String primeiraLinha = c.getDescricao() != null ? c.getDescricao().lines().findFirst().orElse("").trim() : "";
                System.out.println(String.format("  %-20s : %s", c.getNome(), primeiraLinha));
            }
            System.out.println("\nDigite ++ para ver todos os comandos do sistema.");
        } else {
            System.out.println("\n=== COMANDOS PRINCIPAIS ===");
            for (ComandoPainel c : comandosGerais.listar()) {
                String primeiraLinha = c.getDescricao() != null ? c.getDescricao().lines().findFirst().orElse("").trim() : "";
                System.out.println(String.format("  %-20s : %s", c.getNome(), primeiraLinha));
            }
        }
    }

    public void exibirAjudaDetalhada(String nomeComando) {
        String formatado = nomeComando.startsWith("/") ? nomeComando : "/" + nomeComando;
        ComandoPainel comando = null;
        if (temOrcamentoAtivo()) {
            comando = comandosOrcamento.buscar(formatado);
        }
        if (comando == null) {
            comando = comandosGerais.buscar(formatado);
        }

        if (comando == null) {
            System.out.println("Comando não encontrado: " + formatado);
            return;
        }

        System.out.println("\nComando: " + comando.getNome());
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(comando.getDescricao().trim());
        System.out.println("--------------------------------------------------------------------------------");
    }

    public void definirOpcoesProdutosPendentes(List<Produto> produtos, int quantidade, BigDecimal preco) {
        this.opcoesProdutosPendentes = produtos;
        this.quantidadePendente = quantidade;
        this.precoPendente = preco;
    }

    public boolean temOpcoesProdutosPendentes() {
        return opcoesProdutosPendentes != null && !opcoesProdutosPendentes.isEmpty();
    }

    public void definirOpcoesAbrirPendentes(List<Orcamento> orcamentos) {
        this.opcoesOrcamentosParaAbrir = orcamentos;
    }

    public boolean temOpcoesAbrirPendentes() {
        return opcoesOrcamentosParaAbrir != null && !opcoesOrcamentosParaAbrir.isEmpty();
    }

    public void definirOpcoesRecuperarPendentes(List<Orcamento> orcamentos) {
        this.opcoesOrcamentosParaRecuperar = orcamentos;
    }

    public boolean temOpcoesRecuperarPendentes() {
        return opcoesOrcamentosParaRecuperar != null && !opcoesOrcamentosParaRecuperar.isEmpty();
    }

    public void definirOpcoesDescartarPendentes(List<Orcamento> orcamentos) {
        this.opcoesOrcamentosParaDescartar = orcamentos;
    }

    public boolean temOpcoesDescartarPendentes() {
        return opcoesOrcamentosParaDescartar != null && !opcoesOrcamentosParaDescartar.isEmpty();
    }

    public void limparOpcoesPendentes() {
        this.opcoesProdutosPendentes = null;
        this.opcoesOrcamentosParaAbrir = null;
        this.opcoesOrcamentosParaRecuperar = null;
        this.opcoesOrcamentosParaDescartar = null;
    }

    private boolean processarSelecaoOpcaoProduto(String linha) {
        String[] partes = linha.split("\\s+");
        try {
            int indice = Integer.parseInt(partes[0]);
            if (indice >= 1 && indice <= opcoesProdutosPendentes.size()) {
                Produto selecionado = opcoesProdutosPendentes.get(indice - 1);
                int qtd = partes.length > 1 ? Integer.parseInt(partes[1]) : this.quantidadePendente;
                BigDecimal preco = this.precoPendente != null ? this.precoPendente : selecionado.getPrecoVenda();

                limparOpcoesPendentes();
                adicionarProdutoDireto(selecionado, qtd, preco);
                return true;
            }
        } catch (NumberFormatException e) {
            // Se o usuário digitou outro comando, cancela a seleção pendente e segue o fluxo
            limparOpcoesPendentes();
            return false;
        }
        limparOpcoesPendentes();
        return false;
    }

    private boolean processarSelecaoOpcaoAbrir(String linha) {
        try {
            int indice = Integer.parseInt(linha.trim());
            if (indice >= 1 && indice <= opcoesOrcamentosParaAbrir.size()) {
                Orcamento selecionado = opcoesOrcamentosParaAbrir.get(indice - 1);
                limparOpcoesPendentes();
                abrirOrcamento(selecionado.getIdOrcamento());
                return true;
            }
        } catch (NumberFormatException e) {
            limparOpcoesPendentes();
            return false;
        }
        limparOpcoesPendentes();
        return false;
    }

    private boolean processarSelecaoOpcaoRecuperar(String linha) {
        try {
            int indice = Integer.parseInt(linha.trim());
            if (indice >= 1 && indice <= opcoesOrcamentosParaRecuperar.size()) {
                Orcamento selecionado = opcoesOrcamentosParaRecuperar.get(indice - 1);
                limparOpcoesPendentes();
                recuperarOrcamento(selecionado.getIdOrcamento());
                return true;
            }
        } catch (NumberFormatException e) {
            limparOpcoesPendentes();
            return false;
        }
        limparOpcoesPendentes();
        return false;
    }

    private boolean processarSelecaoOpcaoDescartar(String linha) {
        try {
            int indice = Integer.parseInt(linha.trim());
            if (indice >= 1 && indice <= opcoesOrcamentosParaDescartar.size()) {
                Orcamento selecionado = opcoesOrcamentosParaDescartar.get(indice - 1);
                limparOpcoesPendentes();
                descartarOrcamento(selecionado.getIdOrcamento());
                return true;
            }
        } catch (NumberFormatException e) {
            limparOpcoesPendentes();
            return false;
        }
        limparOpcoesPendentes();
        return false;
    }

    public void adicionarProdutoDireto(Produto produto, int quantidade, BigDecimal precoUnitario) {
        if (!temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo para adicionar item.");
            return;
        }

        try {
            ItemOrcamento item = orcamentoDAO.adicionarItem(idOrcamentoAtivo, produto, quantidade, precoUnitario);
            Orcamento orcAtualizado = getOrcamentoAtivo();

            System.out.println("\n[ITEM ADICIONADO AO ORÇAMENTO]");
            System.out.println(String.format("Item %02d: %s (x%d)", item.getNumeroItem(), item.getNomeProduto(), item.getQuantidade()));
            System.out.println(String.format("Preço Un: R$ %.2f | Subtotal do Item: R$ %.2f", item.getPrecoUnitario(), item.getValorItem()));
            System.out.println(String.format("Total Atualizado do Orçamento: R$ %.2f", orcAtualizado.getValorTotal()));

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("\nErro ao adicionar item: " + e.getMessage());
        }
    }

    public void abrirOrcamento(int idOrcamento) {
        try {
            Orcamento orc = orcamentoDAO.buscarPorId(idOrcamento);
            if (orc == null) {
                System.out.println("Orçamento #" + idOrcamento + " não encontrado.");
                return;
            }

            if (orc.getStatusOrcamento() == StatusOrcamento.FINALIZADO) {
                System.out.println("Orçamentos FINALIZADOS não podem ser abertos para edição. Use /duplicar para criar um novo a partir deste.");
                return;
            }

            if (orc.getStatusOrcamento() == StatusOrcamento.CONFIRMADO || orc.getStatusOrcamento() == StatusOrcamento.CANCELADO) {
                orcamentoDAO.atualizarStatus(idOrcamento, StatusOrcamento.ABERTO);
                orc = orcamentoDAO.buscarPorId(idOrcamento);
            }

            entrarOrcamento(orc.getIdOrcamento());

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Erro ao abrir orçamento: " + e.getMessage());
        }
    }

    public void recuperarOrcamento(int idOrcamento) {
        try {
            Orcamento orc = orcamentoDAO.buscarPorId(idOrcamento);
            if (orc == null) {
                System.out.println("Orçamento #" + idOrcamento + " não encontrado.");
                return;
            }

            if (orc.getStatusOrcamento() == StatusOrcamento.CANCELADO) {
                orcamentoDAO.atualizarStatus(idOrcamento, StatusOrcamento.ABERTO);
                orc = orcamentoDAO.buscarPorId(idOrcamento);
                System.out.println(String.format("\n[ORÇAMENTO #%d RECUPERADO COM SUCESSO - STATUS: ABERTO]", idOrcamento));
            }

            entrarOrcamento(orc.getIdOrcamento());

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Erro ao recuperar orçamento: " + e.getMessage());
        }
    }

    public void descartarOrcamento(int idOrcamento) {
        try {
            Orcamento orc = orcamentoDAO.buscarPorId(idOrcamento);
            if (orc == null) {
                System.out.println("Orçamento #" + idOrcamento + " não encontrado.");
                return;
            }

            if (orc.getStatusOrcamento() == StatusOrcamento.CANCELADO) {
                System.out.println("O orçamento #" + idOrcamento + " já está CANCELADO.");
                return;
            }

            if (orc.getStatusOrcamento() == StatusOrcamento.FINALIZADO) {
                System.out.println("Orçamentos FINALIZADOS não podem ser descartados.");
                return;
            }

            if (!orc.podeCancelar()) {
                System.out.println("Não é possível descartar um orçamento no status " + orc.getStatusOrcamento() + ".");
                return;
            }

            String nome;
            try {
                nome = orc.getNomeOrcamento();
            } catch (Exception e) {
                nome = "Sem Nome";
            }

            System.out.print(String.format("Tem certeza que deseja descartar o orçamento #%d - %s? (s/n): ", idOrcamento, nome));
            String resp = entrada.nextLine().trim().toLowerCase();

            if (!resp.equals("s") && !resp.equals("sim")) {
                System.out.println("Operação de descarte cancelada.");
                return;
            }

            boolean cancelou = orcamentoDAO.atualizarStatus(idOrcamento, StatusOrcamento.CANCELADO);
            if (cancelou) {
                System.out.println(String.format("\n[ORÇAMENTO #%d DESCARTADO COM SUCESSO - STATUS: CANCELADO]", idOrcamento));
                System.out.println("Você pode reabrir este orçamento a qualquer momento utilizando o comando /recuperar.");
                if (temOrcamentoAtivo() && getIdOrcamentoAtivo() == idOrcamento) {
                    sairOrcamento();
                }
            } else {
                System.out.println("Não foi possível descartar o orçamento.");
            }

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Erro ao descartar orçamento: " + e.getMessage());
        }
    }

    public void imprimirResumoCompleto(Orcamento orc) {
        if (orc == null) {
            System.out.println("Orçamento não informado.");
            return;
        }

        String nome;
        try {
            nome = orc.getNomeOrcamento();
        } catch (Exception e) {
            nome = "Não informado";
        }

        String clienteStr = orc.temCliente()
                ? String.format("%s (ID: %d | CPF: %s)", orc.getCliente().getNomeCliente(), orc.getCliente().getIdCliente(), orc.getCliente().getCpf())
                : "Não informado";

        System.out.println("\n====================================================================================================");
        System.out.println(centralizarTexto(String.format("ORÇAMENTO #%04d", orc.getIdOrcamento()), 100));
        System.out.println("====================================================================================================");
        System.out.println(String.format(" Nome: %-42s Status: %s", nome, orc.getStatusOrcamento()));
        System.out.println(String.format(" Data: %-42s Cliente: %s", orc.getDataOrcamento().format(FORMATO_DATA), clienteStr));
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println(String.format(" %-4s | %-37s | %-4s | %-13s | %-13s | %-13s",
                "Item", "Produto", "Qtd", "Unit. Bruto", "Unit. Líquido", "Total Item"));
        System.out.println("----------------------------------------------------------------------------------------------------");

        if (orc.getItensOrcamento().isEmpty()) {
            System.out.println("                                      Nenhum item cadastrado.                                       ");
        } else {
            for (ItemOrcamento item : orc.getItensOrcamento()) {
                List<String> linhasNome = quebrarTexto(item.getNomeProduto(), 37);

                for (int k = 0; k < linhasNome.size(); k++) {
                    String itemStr = (k == 0) ? String.format(" %02d ", item.getNumeroItem()) : "    ";
                    String qtdStr = (k == 0) ? String.format("%4d", item.getQuantidade()) : "    ";
                    String brutoStr = (k == 0) ? String.format("R$ %9.2f", item.getPrecoUnitarioTabela()) : "";
                    String liquidoStr = (k == 0) ? String.format("R$ %9.2f", item.getPrecoUnitarioLiquido()) : "";
                    String totalStr = (k == 0) ? String.format("R$ %9.2f", item.getValorItem()) : "";

                    System.out.println(String.format(" %-4s | %-37s | %-4s | %13s | %13s | %13s",
                            itemStr, linhasNome.get(k), qtdStr, brutoStr, liquidoStr, totalStr));
                }
            }
        }

        BigDecimal valorBruto = orc.getValorBruto();
        BigDecimal valorLiquido = orc.getValorTotal();
        BigDecimal valorDesconto = orc.getValorDesconto();

        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println(String.format(" %-20s %77s ", "Valor Bruto:", String.format("R$ %9.2f", valorBruto)));
        System.out.println(String.format(" %-20s %77s ", "Descontos:", String.format("- R$ %9.2f", valorDesconto)));
        System.out.println(String.format(" %-20s %77s ", "Valor Líquido:", String.format("R$ %9.2f", valorLiquido)));
        System.out.println("====================================================================================================");
        System.out.println(String.format(" %-20s %77s ", "VALOR TOTAL:", String.format("R$ %9.2f", orc.getValorTotal())));
        System.out.println("====================================================================================================");
    }

    private static String centralizarTexto(String texto, int larguraTotal) {
        if (texto == null) {
            return "";
        }
        if (texto.length() >= larguraTotal) {
            return texto;
        }
        int espacosEsquerda = (larguraTotal - texto.length()) / 2;
        int espacosDireita = larguraTotal - texto.length() - espacosEsquerda;
        return " ".repeat(espacosEsquerda) + texto + " ".repeat(espacosDireita);
    }

    private static List<String> quebrarTexto(String texto, int larguraMaxima) {
        List<String> linhas = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) {
            linhas.add("");
            return linhas;
        }

        String[] palavras = texto.trim().split("\\s+");
        StringBuilder linhaAtual = new StringBuilder();

        for (String palavra : palavras) {
            if (palavra.length() > larguraMaxima) {
                if (!linhaAtual.isEmpty()) {
                    linhas.add(linhaAtual.toString());
                    linhaAtual.setLength(0);
                }
                int inicio = 0;
                while (inicio < palavra.length()) {
                    int fim = Math.min(inicio + larguraMaxima, palavra.length());
                    linhas.add(palavra.substring(inicio, fim));
                    inicio = fim;
                }
            } else if (linhaAtual.length() + (linhaAtual.isEmpty() ? 0 : 1) + palavra.length() <= larguraMaxima) {
                if (!linhaAtual.isEmpty()) {
                    linhaAtual.append(" ");
                }
                linhaAtual.append(palavra);
            } else {
                linhas.add(linhaAtual.toString());
                linhaAtual.setLength(0);
                linhaAtual.append(palavra);
            }
        }

        if (!linhaAtual.isEmpty()) {
            linhas.add(linhaAtual.toString());
        }

        return linhas;
    }

    public Scanner getEntrada() {
        return entrada;
    }

    public OrcamentoDAO getOrcamentoDAO() {
        return orcamentoDAO;
    }

    public ProdutoDAO getProdutoDAO() {
        return produtoDAO;
    }

    public ClienteDAO getClienteDAO() {
        return clienteDAO;
    }

    private static String[] separarArgumentos(String linha) {
        List<String> argumentos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char caractere = linha.charAt(i);

            if (caractere == '"') {
                dentroDeAspas = !dentroDeAspas;
                continue;
            }

            if (Character.isWhitespace(caractere) && !dentroDeAspas) {
                if (!atual.isEmpty()) {
                    argumentos.add(atual.toString());
                    atual.setLength(0);
                }
                continue;
            }

            atual.append(caractere);
        }

        if (dentroDeAspas) {
            throw new IllegalArgumentException("Aspas não foram fechadas.");
        }

        if (!atual.isEmpty()) {
            argumentos.add(atual.toString());
        }

        return argumentos.toArray(String[]::new);
    }
}
