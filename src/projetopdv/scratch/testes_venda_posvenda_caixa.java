package projetopdv.scratch;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import projetopdv.caixa.CaixaDAO;
import projetopdv.caixa.MovimentacaoCaixa;
import projetopdv.caixa.TipoMovimentacaoCaixa;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.posvenda.DevolucaoItem;
import projetopdv.posvenda.PosVendaDAO;
import projetopdv.posvenda.StatusValeCompra;
import projetopdv.posvenda.ValeCompra;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.venda.FormaPagamento;
import projetopdv.venda.ItemVenda;
import projetopdv.venda.ModalidadeEstorno;
import projetopdv.venda.Venda;
import projetopdv.venda.Venda.StatusVenda;
import projetopdv.venda.VendaDAO;

public class testes_venda_posvenda_caixa {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("SUITE DE TESTES: VENDA, PÓS-VENDA, DEVOLUÇÕES, VALES-COMPRA E SANGRIA DE CAIXA");
        System.out.println("===============================================================================\n");

        // 1. Inicializar e Resetar Banco de Dados
        System.out.println("[TESTE 1] Inicializando e resetando banco de dados...");
        BancoDeDados.main(new String[]{});
        try {
            BancoDeDados.resetarTudo();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao resetar banco de dados: " + e.getMessage());
        }
        System.out.println("-> OK: Banco limpo e tabelas criadas com sucesso!\n");

        ProdutoDAO produtoDAO = new ProdutoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
        VendaDAO vendaDAO = new VendaDAO();
        PosVendaDAO posVendaDAO = new PosVendaDAO();
        CaixaDAO caixaDAO = new CaixaDAO();

        // 2. Cadastrar Cliente e Produtos
        System.out.println("[TESTE 2] Cadastrando dados base (Cliente e Produtos)...");
        Cliente cliente = clienteDAO.cadastrar("Carlos Eduardo Silva", "20/05/1985", "123.456.789-00");
        Produto prodTeclado = produtoDAO.cadastrar("Teclado Mecânico RGB", "7891001", new BigDecimal("200.00"));
        Produto prodMouse = produtoDAO.cadastrar("Mouse Gamer 16000 DPI", "7891002", new BigDecimal("100.00"));
        assert cliente != null : "Cliente não cadastrado";
        assert prodTeclado != null : "Teclado não cadastrado";
        assert prodMouse != null : "Mouse não cadastrado";
        System.out.println("-> OK: Cliente e 2 Produtos cadastrados.\n");

        // 3. Criar e Confirmar Orçamento com Desconto
        System.out.println("[TESTE 3] Criando Orçamento #1 com desconto em item...");
        // 1 Teclado de R$ 200 por R$ 180 (desconto de R$ 20)
        ItemOrcamento item1 = new ItemOrcamento(1, prodTeclado, prodTeclado.getNomeProduto(), prodTeclado.getCodBarras(),
                new BigDecimal("200.00"), new BigDecimal("180.00"), 1);
        // 3 Mouses de R$ 100 por R$ 90 cada (desconto de R$ 10 cada = total R$ 270)
        ItemOrcamento item2 = new ItemOrcamento(2, prodMouse, prodMouse.getNomeProduto(), prodMouse.getCodBarras(),
                new BigDecimal("100.00"), new BigDecimal("90.00"), 3);

        Orcamento orcamento1 = orcamentoDAO.cadastrar("Orçamento Gamer Carlos", cliente, List.of(item1, item2));
        assert orcamento1 != null : "Orçamento não cadastrado";
        assert orcamento1.getValorTotal().compareTo(new BigDecimal("450.00")) == 0 : "Total esperado: 450.00, obtido: " + orcamento1.getValorTotal();

        try {
            orcamentoDAO.confirmarOrcamento(orcamento1.getIdOrcamento(), null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        orcamento1 = orcamentoDAO.buscarPorId(orcamento1.getIdOrcamento());
        assert orcamento1.getStatusOrcamento() == StatusOrcamento.CONFIRMADO : "Status deveria ser CONFIRMADO";
        System.out.println("-> OK: Orçamento confirmado com Total R$ " + orcamento1.getValorTotal() + " (Bruto: R$ 500, Desconto: R$ 50)\n");

        // 4. Faturamento Atômico via VendaDAO
        System.out.println("[TESTE 4] Faturando Orçamento #1 em DINHEIRO via VendaDAO...");
        Venda venda1 = vendaDAO.faturarOrcamento(orcamento1, "Dinheiro");
        assert venda1 != null : "Falha ao faturar venda";
        assert venda1.getIdVenda() > 0 : "ID da venda inválido";
        assert venda1.getStatusVenda() == StatusVenda.CONCLUIDA : "Status da venda deve ser CONCLUIDA";
        assert venda1.getValorTotal().compareTo(new BigDecimal("450.00")) == 0 : "Valor da venda incorreto";
        assert venda1.getValorSubtotalBruto().compareTo(new BigDecimal("500.00")) == 0 : "Subtotal bruto incorreto";
        assert venda1.getValorTotalDesconto().compareTo(new BigDecimal("50.00")) == 0 : "Desconto total incorreto";
        assert venda1.getQuantidadeTotalItens() == 4 : "Quantidade total de itens incorreta";

        // Validar integridade no banco: Orçamento agora é FINALIZADO e carrega sua Venda
        Orcamento orcamentoAposFaturamento = orcamentoDAO.buscarPorId(orcamento1.getIdOrcamento());
        assert orcamentoAposFaturamento.getStatusOrcamento() == StatusOrcamento.FINALIZADO : "Status do orçamento deveria ser FINALIZADO";
        assert orcamentoAposFaturamento.getVenda() != null : "Orçamento finalizado deve conter o objeto Venda";
        assert orcamentoAposFaturamento.getVenda().getIdVenda() == venda1.getIdVenda() : "ID da venda no orçamento divergente";

        // Validar bloqueio de faturamento duplicado
        boolean bloqueouDuplicado = false;
        try {
            vendaDAO.faturarOrcamento(orcamentoAposFaturamento, "Dinheiro");
        } catch (IllegalStateException e) {
            bloqueouDuplicado = true;
            System.out.println("   (Bloqueio defensivo confirmado: " + e.getMessage() + ")");
        }
        assert bloqueouDuplicado : "Deveria bloquear novo faturamento de orçamento já finalizado";
        System.out.println("-> OK: Venda #1 faturada com sucesso! Vínculo bidirecional com Orçamento verificado.\n");

        // 5. Teste de Devolução Parcial de Item com Geração de Vale-Compra
        System.out.println("[TESTE 5] Processando devolução parcial de 1 Mouse (comprado por R$ 90 com desconto)...");
        ItemVenda itemMouseVenda = venda1.getItensVenda().stream()
                .filter(i -> i.getNomeProduto().contains("Mouse"))
                .findFirst()
                .orElseThrow();

        // Tentar devolver 4 mouses (quando só comprou 3) -> Deve falhar
        boolean bloqueouExcesso = false;
        try {
            posVendaDAO.processarDevolucaoItem(venda1.getIdVenda(), itemMouseVenda.getIdItemVenda(), 4, "Teste", true, cliente.getIdCliente());
        } catch (IllegalArgumentException e) {
            bloqueouExcesso = true;
            System.out.println("   (Bloqueio de quantidade excedente confirmado: " + e.getMessage() + ")");
        }
        assert bloqueouExcesso : "Deveria bloquear tentativa de devolver mais unidades do que as compradas";

        // Devolver 1 mouse validamente
        DevolucaoItem devolucao1 = posVendaDAO.processarDevolucaoItem(
                venda1.getIdVenda(), itemMouseVenda.getIdItemVenda(), 1, "Cliente achou o mouse muito grande", true, cliente.getIdCliente()
        );
        assert devolucao1 != null : "Devolução não processada";
        assert devolucao1.getValorUnitario().compareTo(new BigDecimal("90.00")) == 0 : "O reembolso deve ser sobre o preço líquido pago (R$ 90)";
        assert devolucao1.getValorTotal().compareTo(new BigDecimal("90.00")) == 0 : "Total da devolução incorreto";
        assert devolucao1.getIdValeCompra() != null : "Deveria ter gerado um vale-compra";

        ValeCompra vale1 = posVendaDAO.buscarValePorId(devolucao1.getIdValeCompra());
        assert vale1 != null : "Vale-compra não encontrado no banco";
        assert vale1.getSaldo().compareTo(new BigDecimal("90.00")) == 0 : "Saldo do vale deve ser R$ 90.00";
        assert vale1.getStatusVale() == StatusValeCompra.ATIVO : "Vale deve estar ATIVO";
        System.out.println("-> OK: Devolução processada! " + devolucao1);
        System.out.println("-> OK: " + vale1 + "\n");

        // 6. Teste de Uso de Vale-Compra como Pagamento em Nova Venda
        System.out.println("[TESTE 6] Realizando Nova Venda (#2) utilizando o Vale-Compra como pagamento...");
        // Novo orçamento de 1 Mouse por R$ 50
        Produto mouseBasico = produtoDAO.cadastrar("Mouse Office Básico", "7892001", new BigDecimal("50.00"));
        ItemOrcamento itemNovo = new ItemOrcamento(1, mouseBasico, mouseBasico.getNomeProduto(), mouseBasico.getCodBarras(),
                new BigDecimal("50.00"), 1);
        Orcamento orcamento2 = orcamentoDAO.cadastrar("Compra com Vale Carlos", cliente, List.of(itemNovo));
        try {
            orcamentoDAO.confirmarOrcamento(orcamento2.getIdOrcamento(), null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        orcamento2 = orcamentoDAO.buscarPorId(orcamento2.getIdOrcamento());

        // Faturar usando VALE_COMPRA informando o código
        Venda venda2 = vendaDAO.faturarOrcamento(orcamento2, FormaPagamento.VALE_COMPRA.getDescricao(), vale1.getCodigoVale());
        assert venda2 != null : "Falha ao faturar venda com vale-compra";
        assert venda2.getValorTotal().compareTo(new BigDecimal("50.00")) == 0 : "Total da venda incorreto";

        // Verificar abatimento no vale-compra (tinha R$ 90, usou R$ 50, deve restar R$ 40)
        ValeCompra valeAposUso = posVendaDAO.buscarValePorId(vale1.getIdValeCompra());
        assert valeAposUso.getSaldo().compareTo(new BigDecimal("40.00")) == 0 : "Saldo restante deveria ser R$ 40.00, obtido: " + valeAposUso.getSaldo();
        assert valeAposUso.getStatusVale() == StatusValeCompra.ATIVO : "Vale com saldo deve continuar ATIVO";
        System.out.println("-> OK: Venda #2 paga com sucesso via Vale-Compra! Saldo restante do vale: R$ " + valeAposUso.getSaldo() + "\n");

        // 7. Teste de Sangria de Caixa por Resgate de Vale-Compra em Dinheiro
        System.out.println("[TESTE 7] Cliente solicita resgate em dinheiro dos R$ 40 restantes do vale (Sangria de Caixa)...");
        MovimentacaoCaixa sangriaVale = caixaDAO.registrarSangriaResgateVale(
                valeAposUso.getIdValeCompra(), new BigDecimal("40.00"), "Cliente solicitou devolução do saldo em espécie", 1
        );
        assert sangriaVale != null : "Falha ao registrar sangria de resgate de vale";
        assert sangriaVale.getTipo() == TipoMovimentacaoCaixa.SANGRIA_RESGATE_VALE : "Tipo de movimentação incorreto";
        assert sangriaVale.getValor().compareTo(new BigDecimal("40.00")) == 0 : "Valor da sangria incorreto";
        assert sangriaVale.isSaida() : "Sangria deve ser considerada saída";

        // Verificar que o vale foi totalmente liquidado
        ValeCompra valeLiquidado = posVendaDAO.buscarValePorId(vale1.getIdValeCompra());
        assert valeLiquidado.getSaldo().compareTo(BigDecimal.ZERO) == 0 : "Saldo deve ser zero";
        assert valeLiquidado.getStatusVale() == StatusValeCompra.RESGATADO_SANGRIA : "Status deve ser RESGATADO_SANGRIA";
        System.out.println("-> OK: Sangria de resgate registrada no caixa com sucesso: " + sangriaVale);
        System.out.println("-> OK: Status final do vale: " + valeLiquidado.getStatusVale() + "\n");

        // 8. Teste de Estorno Integral com REEMBOLSO_IMEDIATO (Sem Vale-Compra)
        System.out.println("[TESTE 8] Criando Venda #3 em Dinheiro e realizando Estorno com REEMBOLSO_IMEDIATO...");
        Produto headset = produtoDAO.cadastrar("Headset Gamer 7.1", "7893001", new BigDecimal("350.00"));
        ItemOrcamento itemHeadset = new ItemOrcamento(1, headset, headset.getNomeProduto(), headset.getCodBarras(),
                new BigDecimal("350.00"), 1);
        Orcamento orcamento3 = orcamentoDAO.cadastrar("Compra Headset Dinheiro", cliente, List.of(itemHeadset));
        try {
            orcamentoDAO.confirmarOrcamento(orcamento3.getIdOrcamento(), null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        orcamento3 = orcamentoDAO.buscarPorId(orcamento3.getIdOrcamento());

        Venda venda3 = vendaDAO.faturarOrcamento(orcamento3, "Dinheiro");
        assert venda3 != null : "Venda #3 não faturada";

        // Estornar imediatamente na mesma forma de pagamento
        boolean estornou = vendaDAO.estornarVenda(venda3.getIdVenda(), "Produto incompatível com o console", ModalidadeEstorno.REEMBOLSO_IMEDIATO, 1);
        assert estornou : "Falha ao executar estorno da venda";

        Venda venda3Estornada = vendaDAO.buscarPorId(venda3.getIdVenda());
        assert venda3Estornada.isEstornada() : "Venda deveria estar no status ESTORNADA";
        assert venda3Estornada.getModalidadeEstorno() == ModalidadeEstorno.REEMBOLSO_IMEDIATO : "Modalidade deveria ser REEMBOLSO_IMEDIATO";

        // Validar que gerou movimentação imediata no caixa para saída de dinheiro da gaveta física
        List<MovimentacaoCaixa> movs = caixaDAO.listarTodas();
        boolean temEstornoDinheiro = movs.stream()
                .anyMatch(m -> m.getTipo() == TipoMovimentacaoCaixa.ESTORNO_VENDA_DINHEIRO
                        && m.getValor().compareTo(new BigDecimal("350.00")) == 0);
        assert temEstornoDinheiro : "Deveria ter registrado saída imediata de dinheiro da gaveta no CaixaDAO";

        // Validar que NENHUM vale-compra foi criado
        List<ValeCompra> valesVenda3 = posVendaDAO.listarValesAtivos().stream()
                .filter(v -> v.getIdVendaOrigem() != null && v.getIdVendaOrigem() == venda3.getIdVenda())
                .toList();
        assert valesVenda3.isEmpty() : "Estorno imediato NÃO deve criar vale-compra!";
        System.out.println("-> OK: Venda #3 estornada com REEMBOLSO_IMEDIATO! Dinheiro devolvido e registrado na gaveta sem vales pendentes.\n");

        // 9. Teste de Emissão de Comprovantes e Cupons
        System.out.println("[TESTE 9] Validando emissão textual de Cupom Fiscal, Comprovante de Estorno e Vale...");
        String cupom = venda1.gerarCupomFiscal();
        assert cupom.contains("CUPOM NÃO FISCAL - PDV") : "Cupom inválido";
        assert cupom.contains("SUBTOTAL BRUTO") : "Cupom deve conter subtotal bruto";
        assert cupom.contains("DESCONTO TOTAL CONCEDIDO") : "Cupom deve conter desconto total";

        String comprovanteEstorno = venda3Estornada.gerarComprovanteEstorno();
        assert comprovanteEstorno.contains("COMPROVANTE DE ESTORNO") : "Comprovante de estorno inválido";
        assert comprovanteEstorno.contains("REEMBOLSO_IMEDIATO") || comprovanteEstorno.contains("Reembolso Imediato") : "Comprovante deve conter a modalidade";

        String comprovanteVale = vale1.gerarComprovante();
        assert comprovanteVale.contains("VALE-COMPRA / CRÉDITO") : "Comprovante de vale inválido";

        System.out.println("--- PRÉVIA DO CUPOM DA VENDA #1 ---");
        System.out.println(cupom);

        System.out.println("--- PRÉVIA DO COMPROVANTE DE ESTORNO DA VENDA #3 ---");
        System.out.println(comprovanteEstorno);

        System.out.println("===============================================================================");
        System.out.println("TODOS OS TESTES FORAM EXECUTADOS COM SUCESSO! 100% DE INTEGRIDADE CONFIRMADA!");
        System.out.println("===============================================================================");
    }
}
