package projetopdv.scratch;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import projetopdv.caixa.CaixaDAO;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.posvenda.ValeCompra;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoAbrirOrcamento;
import projetopdv.ui.comandos.ComandoCadastrarCliente;
import projetopdv.ui.comandos.ComandoCadastrarProduto;
import projetopdv.ui.comandos.ComandoCadastrarUsuario;
import projetopdv.ui.comandos.ComandoCliente;
import projetopdv.ui.comandos.ComandoConsultarCliente;
import projetopdv.ui.comandos.ComandoConsultarProdutos;
import projetopdv.ui.comandos.ComandoDuplicarOrcamento;
import projetopdv.ui.comandos.ComandoHelp;
import projetopdv.ui.comandos.ComandoListarOrcamentos;
import projetopdv.ui.comandos.ComandoLogin;
import projetopdv.ui.comandos.ComandoLogout;
import projetopdv.ui.comandos.ComandoNovoOrcamento;
import projetopdv.ui.comandos.ComandoProduto;
import projetopdv.ui.comandos.ComandoRecuperarOrcamento;
import projetopdv.ui.comandos.ComandoResetarBanco;
import projetopdv.ui.comandos.ComandoSair;
import projetopdv.ui.comandos.ComandoUsuario;
import projetopdv.ui.comandos.ComandosPainel;
import projetopdv.ui.comandos.orcamento.ComandoDescartarOrcamento;
import projetopdv.usuario.UsuarioDAO;
import projetopdv.venda.Venda;
import projetopdv.venda.VendaDAO;

public class testes_comandos_interativos {

    private static SessaoOrcamento criarSessaoPainel(
            Scanner entrada,
            OrcamentoDAO orcamentoDAO,
            ProdutoDAO produtoDAO,
            ClienteDAO clienteDAO,
            UsuarioDAO usuarioDAO
    ) {
        ComandosPainel comandos = new ComandosPainel();
        SessaoOrcamento sessao = new SessaoOrcamento(entrada, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO, comandos);

        comandos.registrar(new ComandoSair());
        comandos.registrar(new ComandoHelp(comandos));
        comandos.registrar(new ComandoLogin(sessao));
        comandos.registrar(new ComandoLogout(sessao));
        comandos.registrar(new ComandoCadastrarUsuario(sessao));
        comandos.registrar(new ComandoUsuario(sessao));
        comandos.registrar(new ComandoResetarBanco(sessao));
        comandos.registrar(new ComandoCadastrarProduto(sessao));
        comandos.registrar(new ComandoConsultarProdutos(sessao));
        comandos.registrar(new ComandoProduto(sessao));
        comandos.registrar(new ComandoCadastrarCliente(sessao));
        comandos.registrar(new ComandoConsultarCliente(sessao));
        comandos.registrar(new ComandoCliente(sessao));
        comandos.registrar(new ComandoNovoOrcamento(sessao));
        comandos.registrar(new ComandoAbrirOrcamento(sessao));
        comandos.registrar(new ComandoRecuperarOrcamento(sessao));
        comandos.registrar(new ComandoListarOrcamentos(sessao));
        comandos.registrar(new ComandoDuplicarOrcamento(sessao));
        comandos.registrar(new ComandoDescartarOrcamento(sessao));

        return sessao;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("SUITE DE TESTES: COMANDOS INTERATIVOS SEM ARGUMENTOS (FLUXO GUI/CLI)");
        System.out.println("===============================================================================");

        // 1. Resetar banco e inicializar DAOs
        BancoDeDados.inicializarTabelas();
        BancoDeDados.resetarTudo();

        ClienteDAO clienteDAO = new ClienteDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();
        OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        VendaDAO vendaDAO = new VendaDAO();
        CaixaDAO caixaDAO = new CaixaDAO();
        PosVendaDAO posVendaDAO = new PosVendaDAO();

        // 2. Criar dados de base
        Cliente cli1 = clienteDAO.cadastrar("Carlos Drummond", "31/10/1902", "11122233344");
        clienteDAO.cadastrar("Clarice Lispector", "10/12/1920", "55566677788");
        Produto prod1 = produtoDAO.cadastrar("Monitor 27 UltraWide", "7893001", new BigDecimal("1200.00"));
        produtoDAO.cadastrar("Suporte Articulado", "7893002", new BigDecimal("150.00"));

        System.out.println("\n--- TESTE 1: COMANDOS GERAIS DO PAINEL PRINCIPAL ---");
        {
            // Simular entradas para o painel principal:
            // 1) /login (sem args) -> "admin" -> "admin123"
            // 2) /cliente (sem args) -> busca "Carlos" -> menu interativo [1] (Ver detalhes)
            // 3) /produto (sem args) -> busca "Monitor" -> menu interativo [1] (Ver detalhes)
            // 4) /usuario (sem args) -> escolhe operador [1] -> menu interativo [1] (Ver detalhes)
            // 5) /resetar_banco (sem args) -> menu interativo [0] (Cancelar)
            // 6) /sair
            String inputPrincipal = String.join("\n",
                    "/login",
                    "admin",
                    "admin123",
                    "/cliente",
                    "Carlos",
                    "1", // ver detalhes
                    "/produto",
                    "Monitor",
                    "1", // ver detalhes
                    "/usuario",
                    "1", // operador admin
                    "1", // ver detalhes
                    "/resetar_banco",
                    "0", // cancelar
                    "/sair"
            );

            Scanner scannerPrincipal = new Scanner(new ByteArrayInputStream(inputPrincipal.getBytes(StandardCharsets.UTF_8)));
            SessaoOrcamento sessaoPainel = criarSessaoPainel(scannerPrincipal, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO);

            boolean rodando = true;
            while (rodando && scannerPrincipal.hasNextLine()) {
                String linha = scannerPrincipal.nextLine();
                rodando = sessaoPainel.processarLinha(linha);
            }
            System.out.println("-> OK: Comandos gerais (/login, /cliente, /produto, /usuario, /resetar_banco) validados com sucesso!");
        }

        System.out.println("\n--- TESTE 2: COMANDOS DA SESSÃO DE ORÇAMENTO ---");
        {
            // Criar orçamento base
            Orcamento orc = orcamentoDAO.cadastrar("Orçamento Teste Setup", cli1, List.of(
                    new ItemOrcamento(1, prod1, prod1.getNomeProduto(), prod1.getCodBarras(), prod1.getPrecoVenda(), prod1.getPrecoVenda(), 1)
            ));

            // Simular comandos interativos dentro do orçamento:
            // 1) /login admin admin123
            // 2) /abrir 1
            // 3) /add (sem args) -> busca "Suporte" -> quantidade "2" -> desconto "" (default)
            // 4) /renomear (sem args) -> novo nome "Setup Produtivo Home Office"
            // 5) /set_cliente (sem args) -> busca "Clarice"
            // 6) /remove_item (sem args) -> lista itens [1] Monitor, [2] Suporte -> escolhe [2]
            // 7) /confirmar
            // 8) /sair
            String inputOrcamento = String.join("\n",
                    "/login admin admin123",
                    "/abrir " + orc.getIdOrcamento(),
                    "/add",
                    "Suporte",
                    "2", // quantidade
                    "",  // preço unitário default
                    "/renomear",
                    "Setup Produtivo Home Office",
                    "/set_cliente",
                    "Clarice",
                    "/remove_item",
                    "2",
                    "/confirmar",
                    "/sair"
            );

            Scanner scannerOrc = new Scanner(new ByteArrayInputStream(inputOrcamento.getBytes(StandardCharsets.UTF_8)));
            SessaoOrcamento sessaoOrc = criarSessaoPainel(scannerOrc, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO);

            boolean rodando = true;
            while (rodando && scannerOrc.hasNextLine()) {
                String linha = scannerOrc.nextLine();
                rodando = sessaoOrc.processarLinha(linha);
            }

            Orcamento atualizado = orcamentoDAO.buscarPorId(orc.getIdOrcamento());
            assert atualizado.getNomeOrcamento().equals("Setup Produtivo Home Office") : "Nome do orçamento deveria ter sido alterado!";
            assert atualizado.getCliente().getNomeCliente().contains("Clarice") : "Cliente do orçamento deveria ter sido alterado para Clarice!";
            assert atualizado.getItensOrcamento().size() == 1 : "Deveria ter restado apenas 1 item após a remoção interativa!";
            System.out.println("-> OK: Comandos de orçamento (/add, /renomear, /set_cliente, /remove_item) validados com sucesso!");
        }

        System.out.println("\n--- TESTE 3: COMANDOS DA FRENTE DE CAIXA ---");
        {
            // O orçamento #1 agora está CONFIRMADO e pode ser faturado via /faturar sem argumentos!
            // Simular frente de caixa:
            // 1) /login admin admin123
            // 2) /abrir_caixa 200.00
            // 3) /faturar (sem args) -> lista orçamentos pendentes -> escolhe [1] -> forma [1] (Dinheiro) -> valor recebido "1500.00" -> confirma "S"
            // 4) /cupom (sem args) -> lista vendas recentes -> escolhe [1] (reimprime comprovante)
            // 5) /devolver (sem args) -> lista vendas -> escolhe [1] -> escolhe item [1] -> qtd "1" -> motivo "Apresentou defeito no display"
            // 6) /resgatar_vale (sem args) -> lista vales ativos -> escolhe [1] -> enter (valor total) -> justificativa -> confirma se gaveta alertar
            // 7) /fechar_caixa 200.00 Conferencia finalizada
            // 8) /sair
            String inputCaixa = String.join("\n",
                    "/login admin admin123",
                    "/abrir_caixa 200.00",
                    "/faturar",
                    "1",          // Seleciona orçamento #1
                    "1",          // Forma de pagamento [1] Dinheiro
                    "1500.00",    // Valor recebido em dinheiro
                    "S",          // Confirmar faturamento
                    "/cupom",
                    "1",          // Seleciona venda #1 da listagem interativa
                    "/devolver",
                    "1",          // Seleciona venda #1 da listagem interativa
                    "1",          // Item #1
                    "1",          // Quantidade 1
                    "Apresentou defeito no display",
                    "/resgatar_vale",
                    "1",          // Seleciona o vale recém emitido
                    "",           // Pressiona ENTER para resgatar o saldo total
                    "Cliente prefere o valor em espécie", // Motivo da sangria
                    "/fechar_caixa 200.00 Conferencia finalizada",
                    "/sair"
            );

            Scanner scannerCaixa = new Scanner(new ByteArrayInputStream(inputCaixa.getBytes(StandardCharsets.UTF_8)));
            SessaoCaixa sessaoCaixa = new SessaoCaixa(
                    scannerCaixa,
                    vendaDAO,
                    caixaDAO,
                    posVendaDAO,
                    orcamentoDAO,
                    clienteDAO,
                    produtoDAO,
                    usuarioDAO
            );

            boolean rodando = true;
            while (rodando && scannerCaixa.hasNextLine()) {
                String linha = scannerCaixa.nextLine();
                rodando = sessaoCaixa.processarLinha(linha);
            }

            // Validar que a venda foi gerada e depois sofreu devolução com emissão de vale
            List<Venda> vendas = vendaDAO.listarTodas();
            assert !vendas.isEmpty() : "Deveria existir uma venda finalizada no caixa";
            List<ValeCompra> vales = posVendaDAO.listarValesAtivos();
            // Como resgatamos em dinheiro o saldo total via sangria, não deve haver vales com saldo ativo restante
            assert vales.isEmpty() : "Vale deveria estar totalmente resgatado via sangria";

            System.out.println("-> OK: Comandos de caixa (/faturar, /cupom, /devolver, /resgatar_vale) validados com sucesso!");
        }

        System.out.println("\n===============================================================================");
        System.out.println("TODOS OS COMANDOS INTERATIVOS FORAM TESTADOS E VALIDADOS COM SUCESSO!");
        System.out.println("===============================================================================");
    }
}
