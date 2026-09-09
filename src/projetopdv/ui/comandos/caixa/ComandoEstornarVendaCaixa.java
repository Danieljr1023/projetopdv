package projetopdv.ui.comandos.caixa;

import java.util.List;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;
import projetopdv.venda.ModalidadeEstorno;
import projetopdv.venda.Venda;

public class ComandoEstornarVendaCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoEstornarVendaCaixa(SessaoCaixa sessao) {
        super(
                "/estornar",
                """
                Realiza o estorno integral de uma venda com reembolso imediato ou vale-compra.

                Usos:
                /estornar            (Modo interativo: lista vendas elegíveis para seleção rápida)
                /estornar <ID Venda> (Estorno direto pelo ID)
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.VENDA_ESTORNAR)) {
            return true;
        }
        if (!sessao.validarCaixaAberto()) {
            return true;
        }

        int idVenda;
        if (argumentos.length > 0) {
            try {
                idVenda = Integer.parseInt(argumentos[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("ID da venda inválido. Exemplo: /estornar 3");
                return true;
            }
        } else {
            List<Venda> vendas = sessao.getVendaDAO().listarTodas().stream()
                    .filter(v -> !v.isEstornada())
                    .toList();
            if (vendas.isEmpty()) {
                System.out.println("Nenhuma venda elegível para estorno encontrada.");
                return true;
            }

            int limite = Math.min(10, vendas.size());
            List<Venda> recentes = vendas.subList(0, limite);

            System.out.println("\n==========================================================================================");
            System.out.printf("                VENDAS ELEGÍVEIS PARA ESTORNO (%d mais recentes)                         %n", limite);
            System.out.println("==========================================================================================");
            System.out.printf("%-5s | %-6s | %-19s | %-12s | %-15s%n", "OPÇÃO", "ID", "DATA / HORA", "TOTAL", "PAGAMENTO");
            System.out.println("------------------------------------------------------------------------------------------");
            for (int i = 0; i < recentes.size(); i++) {
                Venda v = recentes.get(i);
                System.out.printf("[%2d]  | #%-5d | %-19s | R$ %9.2f | %-15s%n",
                        (i + 1), v.getIdVenda(), v.getDataVenda().format(SessaoCaixa.FORMATO_DATA_HORA),
                        v.getValorTotal(), v.getFormaPagamento());
            }
            System.out.println("==========================================================================================");
            System.out.print("Escolha o número da opção desejada ou informe o ID da venda (ou 'cancelar'): ");
            String input = sessao.getEntrada().nextLine().trim();
            if (input.isEmpty() || input.equalsIgnoreCase("cancelar")) {
                System.out.println("Estorno cancelado.");
                return true;
            }
            try {
                int sel = Integer.parseInt(input);
                if (sel >= 1 && sel <= recentes.size()) {
                    idVenda = recentes.get(sel - 1).getIdVenda();
                } else {
                    idVenda = sel;
                }
            } catch (NumberFormatException e) {
                System.out.println("ID ou opção inválida.");
                return true;
            }
        }

        Venda venda = sessao.getVendaDAO().buscarPorId(idVenda);
        if (venda == null) {
            System.out.printf("[ERRO] Venda #%d não encontrada.%n", idVenda);
            return true;
        }

        if (venda.isEstornada()) {
            System.out.printf("[AVISO] A venda #%d já foi estornada anteriormente!%n", idVenda);
            System.out.println(venda.gerarComprovanteEstorno());
            return true;
        }

        System.out.println("\n====================================================");
        System.out.printf("           ESTORNO TOTAL DA VENDA #%06d            %n", idVenda);
        System.out.println("====================================================");
        System.out.printf("DATA DA COMPRA:     %s%n", venda.getDataVenda().format(SessaoCaixa.FORMATO_DATA_HORA));
        System.out.printf("VALOR DA VENDA:     R$ %.2f%n", venda.getValorTotal());
        System.out.printf("FORMA DE PAGAMENTO: %s%n", venda.getFormaPagamento());
        System.out.println("----------------------------------------------------");

        System.out.print("Informe o motivo do estorno: ");
        String motivo = sessao.getEntrada().nextLine().trim();
        if (motivo.isEmpty()) {
            System.out.println("O motivo do estorno é obrigatório.");
            return true;
        }

        System.out.println("\nMODALIDADE DE ESTORNO DESEJADA:");
        System.out.println("  [1] Reembolso Imediato (devolução na mesma forma de pagamento original)");
        System.out.println("  [2] Vale-Compra (crédito de loja para compras futuras)");
        System.out.println("  [0] Cancelar Operação");
        System.out.print("Selecione a opção: ");

        String opMod = sessao.getEntrada().nextLine().trim();
        ModalidadeEstorno modalidade;
        switch (opMod) {
            case "1" -> modalidade = ModalidadeEstorno.REEMBOLSO_IMEDIATO;
            case "2" -> modalidade = ModalidadeEstorno.VALE_COMPRA;
            case "0" -> {
                System.out.println("Operação de estorno cancelada.");
                return true;
            }
            default -> {
                System.out.println("Opção inválida.");
                return true;
            }
        }

        System.out.printf("Confirmar estorno da venda #%d com modalidade '%s'? (S/N): ",
                idVenda, modalidade.getDescricao());
        String confirma = sessao.getEntrada().nextLine().trim().toUpperCase();
        if (!confirma.startsWith("S")) {
            System.out.println("Estorno cancelado pelo operador.");
            return true;
        }

        int idOperador = sessao.getUsuarioLogado().getIdUsuario();
        boolean sucesso = sessao.getVendaDAO().estornarVenda(idVenda, motivo, modalidade, idOperador);

        if (sucesso) {
            System.out.println("\n✓ VENDA ESTORNADA COM SUCESSO!\n");
            Venda estornada = sessao.getVendaDAO().buscarPorId(idVenda);
            if (estornada != null) {
                System.out.println(estornada.gerarComprovanteEstorno());
            }
            if (modalidade == ModalidadeEstorno.REEMBOLSO_IMEDIATO) {
                System.out.printf("✓ Devolução realizada diretamente na forma original (%s).%n", venda.getFormaPagamento());
            } else {
                System.out.println("✓ Vale-compra emitido com sucesso! O cliente poderá utilizá-lo com o código gerado.");
            }
        } else {
            System.out.println("[ERRO] Falha ao processar o estorno da venda.");
        }

        return true;
    }
}
