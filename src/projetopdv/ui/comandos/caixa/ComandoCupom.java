package projetopdv.ui.comandos.caixa;

import java.util.List;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.venda.Venda;

public class ComandoCupom extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoCupom(SessaoCaixa sessao) {
        super(
                "/cupom",
                """
                Reimprime o cupom fiscal/comprovante de uma venda realizada.

                Usos:
                /cupom            (Modo interativo: lista vendas recentes para seleção rápida)
                /cupom <ID Venda> (Impressão direta pelo ID)
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarAutenticado()) {
            return true;
        }

        int idVenda;
        if (argumentos.length > 0) {
            try {
                idVenda = Integer.parseInt(argumentos[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("ID da venda inválido. Exemplo: /cupom 5");
                return true;
            }
        } else {
            List<Venda> vendas = sessao.getVendaDAO().listarTodas();
            if (vendas.isEmpty()) {
                System.out.println("Nenhuma venda registrada no sistema.");
                return true;
            }

            int limite = Math.min(10, vendas.size());
            List<Venda> recentes = vendas.subList(0, limite);

            System.out.println("\n==========================================================================================");
            System.out.printf("                       VENDAS RECENTES (%d mais recentes)                                 %n", limite);
            System.out.println("==========================================================================================");
            System.out.printf("%-5s | %-6s | %-19s | %-12s | %-15s | %-12s%n",
                    "OPÇÃO", "ID", "DATA / HORA", "TOTAL", "PAGAMENTO", "SITUAÇÃO");
            System.out.println("------------------------------------------------------------------------------------------");
            for (int i = 0; i < recentes.size(); i++) {
                Venda v = recentes.get(i);
                System.out.printf("[%2d]  | #%-5d | %-19s | R$ %9.2f | %-15s | %-12s%n",
                        (i + 1), v.getIdVenda(), v.getDataVenda().format(SessaoCaixa.FORMATO_DATA_HORA),
                        v.getValorTotal(), v.getFormaPagamento(), v.getStatusVenda().getDescricao());
            }
            System.out.println("==========================================================================================");
            System.out.print("Escolha o número da opção desejada ou informe o ID da venda (ou 'cancelar'): ");
            String input = sessao.getEntrada().nextLine().trim();
            if (input.isEmpty() || input.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação cancelada.");
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

        System.out.println("\n" + venda.gerarCupomFiscal());
        return true;
    }
}
