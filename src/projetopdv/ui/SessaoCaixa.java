package projetopdv.ui;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import projetopdv.caixa.CaixaDAO;
import projetopdv.cliente.ClienteDAO;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.ui.comandos.ComandosPainel;
import projetopdv.ui.comandos.caixa.ComandoAbrirCaixa;
import projetopdv.ui.comandos.caixa.ComandoConsultarValeCaixa;
import projetopdv.ui.comandos.caixa.ComandoCupom;
import projetopdv.ui.comandos.caixa.ComandoDevolverItemCaixa;
import projetopdv.ui.comandos.caixa.ComandoEstornarVendaCaixa;
import projetopdv.ui.comandos.caixa.ComandoFaturar;
import projetopdv.ui.comandos.caixa.ComandoFecharCaixa;
import projetopdv.ui.comandos.caixa.ComandoHelpCaixa;
import projetopdv.ui.comandos.caixa.ComandoListarOrcamentosPendentes;
import projetopdv.ui.comandos.caixa.ComandoListarVendasCaixa;
import projetopdv.ui.comandos.caixa.ComandoLoginCaixa;
import projetopdv.ui.comandos.caixa.ComandoLogoutCaixa;
import projetopdv.ui.comandos.caixa.ComandoResgatarValeCaixa;
import projetopdv.ui.comandos.caixa.ComandoSairCaixa;
import projetopdv.ui.comandos.caixa.ComandoSaldoCaixa;
import projetopdv.ui.comandos.caixa.ComandoSangriaCaixa;
import projetopdv.ui.comandos.caixa.ComandoSuprimentoCaixa;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;
import projetopdv.venda.VendaDAO;

public final class SessaoCaixa {

    public static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Scanner entrada;
    private final VendaDAO vendaDAO;
    private final CaixaDAO caixaDAO;
    private final PosVendaDAO posVendaDAO;
    private final OrcamentoDAO orcamentoDAO;
    private final ClienteDAO clienteDAO;
    private final ProdutoDAO produtoDAO;
    private final UsuarioDAO usuarioDAO;
    private final ComandosPainel comandos;

    private Usuario usuarioLogado = null;

    public SessaoCaixa(Scanner entrada, VendaDAO vendaDAO, CaixaDAO caixaDAO, PosVendaDAO posVendaDAO,
                       OrcamentoDAO orcamentoDAO, ClienteDAO clienteDAO, ProdutoDAO produtoDAO, UsuarioDAO usuarioDAO) {
        this.entrada = entrada;
        this.vendaDAO = vendaDAO;
        this.caixaDAO = caixaDAO;
        this.posVendaDAO = posVendaDAO;
        this.orcamentoDAO = orcamentoDAO;
        this.clienteDAO = clienteDAO;
        this.produtoDAO = produtoDAO;
        this.usuarioDAO = usuarioDAO;
        this.comandos = new ComandosPainel();

        registrarComandos();
    }

    private void registrarComandos() {
        comandos.registrar(new ComandoHelpCaixa(this));
        comandos.registrar(new ComandoLoginCaixa(this));
        comandos.registrar(new ComandoLogoutCaixa(this));
        comandos.registrar(new ComandoSairCaixa());

        // Operações de Turno e Gaveta
        comandos.registrar(new ComandoAbrirCaixa(this));
        comandos.registrar(new ComandoFecharCaixa(this));
        comandos.registrar(new ComandoSaldoCaixa(this));
        comandos.registrar(new ComandoSangriaCaixa(this));
        comandos.registrar(new ComandoSuprimentoCaixa(this));

        // Operações Comerciais
        comandos.registrar(new ComandoListarOrcamentosPendentes(this));
        comandos.registrar(new ComandoFaturar(this));
        comandos.registrar(new ComandoListarVendasCaixa(this));
        comandos.registrar(new ComandoCupom(this));

        // Pós-Venda
        comandos.registrar(new ComandoEstornarVendaCaixa(this));
        comandos.registrar(new ComandoDevolverItemCaixa(this));
        comandos.registrar(new ComandoConsultarValeCaixa(this));
        comandos.registrar(new ComandoResgatarValeCaixa(this));
    }

    public String obterPrompt() {
        if (usuarioLogado == null) {
            return "\n[PDV - Não Autenticado] > ";
        }
        if (!caixaDAO.isCaixaAberto()) {
            return String.format("\n[PDV - Operador: %s (CAIXA FECHADO)] > ", usuarioLogado.getLogin());
        }
        BigDecimal saldo = caixaDAO.calcularSaldoDinheiroEmCaixa();
        return String.format("\n[PDV - Operador: %s | Gaveta: R$ %.2f] > ", usuarioLogado.getLogin(), saldo);
    }

    public boolean processarLinha(String linha) {
        if (linha == null || linha.trim().isEmpty()) {
            return true;
        }

        String[] tokens = separarArgumentos(linha.trim());
        if (tokens.length == 0) {
            return true;
        }

        String nomeComando = tokens[0].toLowerCase();
        ComandoPainel comando = comandos.buscar(nomeComando);

        if (comando == null) {
            System.out.printf("Comando '%s' não reconhecido. Digite /help para ver a lista de comandos disponíveis.%n", nomeComando);
            return true;
        }

        String[] argumentos = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, argumentos, 0, argumentos.length);

        return comando.executar(argumentos);
    }

    public boolean estaAutenticado() {
        return usuarioLogado != null;
    }

    public boolean validarAutenticado() {
        if (!estaAutenticado()) {
            System.out.println("[ACESSO NEGADO] É necessário fazer login primeiro. Digite: /login <usuario> <senha>");
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

    public boolean validarPermissao(Permissao permissao) {
        if (!validarAutenticado()) {
            return false;
        }
        if (!temPermissao(permissao)) {
            System.out.printf("[ACESSO NEGADO] Você não possui permissão para esta operação (%s).%n", permissao.getDescricao());
            return false;
        }
        return true;
    }

    public boolean validarCaixaAberto() {
        if (!validarAutenticado()) {
            return false;
        }
        if (!caixaDAO.isCaixaAberto()) {
            System.out.println("[CAIXA FECHADO] O caixa físico está fechado no momento.");
            System.out.println("Para iniciar as operações, realize a abertura com: /abrir_caixa <fundo_troco>");
            return false;
        }
        return true;
    }

    public void setUsuarioLogado(Usuario usuario) {
        this.usuarioLogado = usuario;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public Scanner getEntrada() {
        return entrada;
    }

    public VendaDAO getVendaDAO() {
        return vendaDAO;
    }

    public CaixaDAO getCaixaDAO() {
        return caixaDAO;
    }

    public PosVendaDAO getPosVendaDAO() {
        return posVendaDAO;
    }

    public OrcamentoDAO getOrcamentoDAO() {
        return orcamentoDAO;
    }

    public ClienteDAO getClienteDAO() {
        return clienteDAO;
    }

    public ProdutoDAO getProdutoDAO() {
        return produtoDAO;
    }

    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }

    public ComandosPainel getComandos() {
        return comandos;
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

        if (!atual.isEmpty()) {
            argumentos.add(atual.toString());
        }

        return argumentos.toArray(String[]::new);
    }
}
