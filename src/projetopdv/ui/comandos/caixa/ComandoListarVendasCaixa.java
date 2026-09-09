package projetopdv.ui.comandos.caixa;

import java.util.List;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.venda.Venda;

public class ComandoListarVendasCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoListarVendasCaixa(SessaoCaixa sessao) {
        super("/vendas", "Lista o histórico de vendas realizadas.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarAutenticado()) {
            return true;
        }

        List<Venda> vendas = sessao.getVendaDAO().listarTodas();
        if (vendas.isEmpty()) {
            System.out.println("Nenhuma venda registrada até o momento.");
            return true;
        }

        System.out.println("\n==========================================================================================");
        System.out.printf("                        HISTÓRICO DE VENDAS (%d registradas)                              %n", vendas.size());
        System.out.println("==========================================================================================");
        System.out.printf("%-8s | %-19s | %-18s | %-12s | %-12s%n", "VENDA #", "DATA/HORA", "PAGAMENTO", "VALOR TOTAL", "SITUAÇÃO");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Venda v : vendas) {
            System.out.printf("#%06d  | %-19s | %-18s | R$ %9.2f | %-12s%n",
                    v.getIdVenda(),
                    v.getDataVenda().format(SessaoCaixa.FORMATO_DATA_HORA),
                    v.getFormaPagamento(),
                    v.getValorTotal(),
                    v.getStatusVenda().getDescricao()
            );
        }

        System.out.println("==========================================================================================");
        System.out.println("DICA: Para reimprimir o cupom de uma venda, digite: /cupom <ID>");
        return true;
    }
}
