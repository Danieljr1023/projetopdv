package projetopdv.ui.comandos.caixa;

import java.util.List;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;

public class ComandoListarOrcamentosPendentes extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoListarOrcamentosPendentes(SessaoCaixa sessao) {
        super("/orcamentos", "Lista os orçamentos confirmados aguardando pagamento no caixa.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarCaixaAberto()) {
            return true;
        }

        List<Orcamento> todos;
        try {
            todos = sessao.getOrcamentoDAO().listarTodos();
        } catch (java.sql.SQLException e) {
            System.out.println("[ERRO] Falha ao consultar orçamentos: " + e.getMessage());
            return true;
        }
        List<Orcamento> pendentes = todos.stream()
                .filter(o -> o.getStatusOrcamento() == StatusOrcamento.CONFIRMADO)
                .toList();

        if (pendentes.isEmpty()) {
            System.out.println("Nenhum orçamento confirmado aguardando pagamento no momento.");
            return true;
        }

        System.out.println("\n==========================================================================================");
        System.out.printf("               ORÇAMENTOS PRONTOS PARA FATURAMENTO NO CAIXA (%d encontrados)              %n", pendentes.size());
        System.out.println("==========================================================================================");
        System.out.printf("%-6s | %-25s | %-25s | %-6s | %-12s%n", "ID", "NOME DO ORÇAMENTO", "CLIENTE", "ITENS", "VALOR TOTAL");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Orcamento o : pendentes) {
            String nomeCli = o.temCliente() ? o.getCliente().getNomeCliente() : "Consumidor Final";
            if (nomeCli.length() > 25) {
                nomeCli = nomeCli.substring(0, 23) + "..";
            }
            String nomeOrc = o.getNomeOrcamento();
            if (nomeOrc.length() > 25) {
                nomeOrc = nomeOrc.substring(0, 23) + "..";
            }
            int totalItens = o.getItensOrcamento().stream().mapToInt(ItemOrcamento::getQuantidade).sum();

            System.out.printf("#%-5d | %-25s | %-25s | %4d un | R$ %10.2f%n",
                    o.getIdOrcamento(),
                    nomeOrc,
                    nomeCli,
                    totalItens,
                    o.getValorTotal()
            );
        }

        System.out.println("==========================================================================================");
        System.out.println("DICA: Para faturar e receber um orçamento no caixa, digite: /faturar <ID>");
        return true;
    }
}
