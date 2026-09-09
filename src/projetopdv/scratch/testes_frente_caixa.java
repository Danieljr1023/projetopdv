package projetopdv.scratch;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import projetopdv.caixa.CaixaDAO;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoCaixa;
import projetopdv.usuario.UsuarioDAO;
import projetopdv.venda.VendaDAO;

public class testes_frente_caixa {

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("SUITE DE TESTES: FRENTE DE CAIXA (PDV) - TERMINAL DEDICADO");
        System.out.println("===============================================================================");

        // 1. Resetar banco e inicializar tabelas
        System.out.println("\n[PASSO 1] Resetando banco de dados e preparando tabelas...");
        BancoDeDados.inicializarTabelas();
        BancoDeDados.resetarTudo();

        ProdutoDAO produtoDAO = new ProdutoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        VendaDAO vendaDAO = new VendaDAO();
        CaixaDAO caixaDAO = new CaixaDAO();
        PosVendaDAO posVendaDAO = new PosVendaDAO();

        // 2. Criar dados de teste (Produto, Cliente e Orçamento Confirmado)
        System.out.println("[PASSO 2] Cadastrando produtos, cliente e orçamento confirmado no backoffice...");
        Produto headset = produtoDAO.cadastrar("Headset Gamer 7.1", "7892001", new BigDecimal("250.00"));
        Produto mousepad = produtoDAO.cadastrar("Mousepad Speed XL", "7892002", new BigDecimal("50.00"));
        Cliente cliente = clienteDAO.cadastrar("Maria Oliveira", "15/04/1992", "12345678900");

        ItemOrcamento item1 = new ItemOrcamento(1, headset, headset.getNomeProduto(), headset.getCodBarras(), new BigDecimal("250.00"), new BigDecimal("230.00"), 1);
        ItemOrcamento item2 = new ItemOrcamento(2, mousepad, mousepad.getNomeProduto(), mousepad.getCodBarras(), new BigDecimal("50.00"), new BigDecimal("45.00"), 2);
        
        Orcamento orcamento = orcamentoDAO.cadastrar(
                "Orçamento Gamer para Maria",
                cliente,
                java.util.List.of(item1, item2)
        );

        orcamentoDAO.confirmarOrcamento(orcamento.getIdOrcamento(), null);
        orcamento = orcamentoDAO.buscarPorId(orcamento.getIdOrcamento());

        assert orcamento.getStatusOrcamento() == Orcamento.StatusOrcamento.CONFIRMADO : "Status do orçamento deveria ser CONFIRMADO";
        System.out.printf("-> Orçamento #%d cadastrado e CONFIRMADO com total R$ %.2f%n", orcamento.getIdOrcamento(), orcamento.getValorTotal());

        // 3. Simular sessão de frente de caixa via fluxo interativo
        System.out.println("\n[PASSO 3] Executando fluxo de comandos da Frente de Caixa...");

        String comandosSimulados = String.join("\n",
                "/login admin admin123",                    // Login
                "/abrir_caixa 150.00",                     // Abertura de gaveta com R$ 150 de troco
                "/saldo",                                   // Saldo atual (deve ser R$ 150)
                "/orcamentos",                              // Listar orçamentos pendentes
                "/faturar 1 DINHEIRO 350.00",               // Faturar orçamento #1 com R$ 350 em dinheiro (troco R$ 30)
                "/cupom 1",                                 // Imprimir cupom fiscal da venda #1
                "/saldo",                                   // Saldo atual (150 + 320 = R$ 470)
                "/sangria 200.00 Transferencia para cofre",// Sangria operacional de R$ 200
                "/suprimento 50.00 Reforco de moedas",      // Suprimento de troco R$ 50
                "/saldo",                                   // Saldo atual (470 - 200 + 50 = R$ 320)
                "/devolver 1 2 1 Cliente trocou de modelo", // Devolver 1 mousepad da venda #1 (gera vale R$ 45)
                "/vendas",                                  // Listar vendas registradas
                "/fechar_caixa 320.00 Conferência exata",   // Fechar caixa com conferência cega
                "/sair"                                     // Encerrar terminal
        );

        Scanner entradaSimulada = new Scanner(new ByteArrayInputStream(comandosSimulados.getBytes(StandardCharsets.UTF_8)));
        SessaoCaixa sessao = new SessaoCaixa(
                entradaSimulada,
                vendaDAO,
                caixaDAO,
                posVendaDAO,
                orcamentoDAO,
                clienteDAO,
                produtoDAO,
                usuarioDAO
        );

        boolean executando = true;
        int linhasProcessadas = 0;
        while (executando && entradaSimulada.hasNextLine()) {
            String linha = entradaSimulada.nextLine();
            System.out.println("\n[INPUT SIMULADO] > " + linha);
            executando = sessao.processarLinha(linha);
            linhasProcessadas++;
        }

        System.out.println("\n[PASSO 4] Verificações de integridade pós-sessão de caixa...");
        assert linhasProcessadas > 0 : "Deveria ter processado os comandos da sessão de caixa";
        assert !caixaDAO.isCaixaAberto() : "Caixa físico deveria estar fechado após /fechar_caixa";
        System.out.println("-> OK: Caixa físico está com status FECHADO.");

        BigDecimal saldoFinalEsperado = new BigDecimal("320.00");
        BigDecimal saldoGaveta = caixaDAO.calcularSaldoDinheiroEmCaixa();
        assert saldoGaveta.compareTo(saldoFinalEsperado) == 0 :
                String.format("Saldo em gaveta esperado: R$ %.2f, atual: R$ %.2f", saldoFinalEsperado, saldoGaveta);
        System.out.printf("-> OK: Saldo final na gaveta conferido com exatidão: R$ %.2f%n", saldoGaveta);

        assert vendaDAO.buscarPorId(1).getStatusVenda() == projetopdv.venda.Venda.StatusVenda.CONCLUIDA : "Venda #1 deveria estar CONCLUIDA";
        System.out.println("-> OK: Venda #1 gravada e vinculada.");

        System.out.println("\n===============================================================================");
        System.out.println("TESTES DA FRENTE DE CAIXA FINALIZADOS COM 100% DE SUCESSO!");
        System.out.println("===============================================================================");
    }
}
