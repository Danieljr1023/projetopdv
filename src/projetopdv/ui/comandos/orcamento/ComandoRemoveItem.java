package projetopdv.ui.comandos.orcamento;

import java.sql.SQLException;
import java.util.Scanner;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoRemoveItem extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoRemoveItem(SessaoOrcamento sessao) {
        super(
                "/remove_item",
                """
                Remove um item do orçamento ativo pelo seu número ou painel interativo.

                Usos:
                /remove_item                  (Modo interativo com listagem de itens e confirmação)
                /remove_item <Número do Item> (Remoção direta)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_EDITAR_ITENS)) {
            return true;
        }

        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento.");
            return true;
        }

        Orcamento orc = sessao.getOrcamentoAtivo();
        if (orc == null || orc.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            System.out.println("Não é possível remover itens de um orçamento com status " + (orc != null ? orc.getStatusOrcamento() : "inválido") + ".");
            return true;
        }

        if (orc.getItensOrcamento().isEmpty()) {
            System.out.println("O orçamento ativo não possui nenhum item para ser removido.");
            return true;
        }

        int numeroItem;
        Scanner entrada = sessao.getEntrada();

        if (argumentos.length < 1) {
            System.out.println("\n==================================================");
            System.out.println("           REMOVER ITEM DO ORÇAMENTO              ");
            System.out.println("==================================================");
            for (ItemOrcamento it : orc.getItensOrcamento()) {
                System.out.printf("[%02d] %-30s | Qtd: %4d | Total: R$ %8.2f%n",
                        it.getNumeroItem(), it.getNomeProduto(), it.getQuantidade(), it.getValorItem());
            }
            System.out.println("--------------------------------------------------");
            System.out.print("Informe o número do item que deseja remover (ou 'cancelar'): ");
            String input = entrada.nextLine().trim();
            if (input.isEmpty() || input.equalsIgnoreCase("cancelar")) {
                System.out.println("Remoção cancelada.");
                return true;
            }
            try {
                numeroItem = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Número do item inválido.");
                return true;
            }
        } else {
            try {
                numeroItem = Integer.parseInt(argumentos[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Número do item inválido.");
                return true;
            }
        }

        ItemOrcamento itemParaRemover = orc.buscarItemPorNumero(numeroItem);
        if (itemParaRemover == null) {
            System.out.println("Item #" + numeroItem + " não encontrado neste orçamento.");
            return true;
        }

        String nomeProduto = itemParaRemover.getNomeProduto();

        try {
            boolean removido = orcamentoDAO.removerItem(orc.getIdOrcamento(), numeroItem);
            if (removido) {
                Orcamento orcAtualizado = sessao.getOrcamentoAtivo();
                System.out.println("\n[ITEM REMOVIDO DO ORÇAMENTO]");
                System.out.println(String.format("Item %02d: %s removido com sucesso.", numeroItem, nomeProduto));
                System.out.println(String.format("Novo Total do Orçamento: R$ %.2f", orcAtualizado.getValorTotal()));
            } else {
                System.out.println("Não foi possível remover o item #" + numeroItem + ".");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao remover item no banco de dados: " + e.getMessage());
        }

        return true;
    }
}
