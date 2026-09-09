package projetopdv.ui.comandos.caixa;

import java.util.List;
import projetopdv.orcamento.Orcamento;
import projetopdv.posvenda.DevolucaoItem;
import projetopdv.posvenda.ValeCompra;
import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;
import projetopdv.venda.ItemVenda;
import projetopdv.venda.Venda;

public class ComandoDevolverItemCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoDevolverItemCaixa(SessaoCaixa sessao) {
        super(
                "/devolver",
                """
                Processa a devolução parcial de itens específicos de uma venda e emite Vale-Compra.

                Usos:
                /devolver                           (Modo interativo: lista vendas para seleção e itens a devolver)
                /devolver <ID Venda>                (Exibe os itens da venda para devolução)
                /devolver <ID Venda> <Nº Item> [Qtd] (Devolução direta)
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
                System.out.println("ID da venda inválido. Exemplo: /devolver 5");
                return true;
            }
        } else {
            List<Venda> vendas = sessao.getVendaDAO().listarTodas().stream()
                    .filter(v -> !v.isEstornada())
                    .toList();
            if (vendas.isEmpty()) {
                System.out.println("Nenhuma venda disponível para devolução encontrada.");
                return true;
            }

            int limite = Math.min(10, vendas.size());
            List<Venda> recentes = vendas.subList(0, limite);

            System.out.println("\n==========================================================================================");
            System.out.printf("                VENDAS RECENTES DISPONÍVEIS PARA DEVOLUÇÃO (%d encontradas)               %n", limite);
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
                System.out.println("Devolução cancelada.");
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
            System.out.printf("[AVISO] A venda #%d já foi estornada integralmente.%n", idVenda);
            return true;
        }

        System.out.println("\n==========================================================================================");
        System.out.printf("                    ITENS DA VENDA #%06d DISPONÍVEIS PARA DEVOLUÇÃO                      %n", idVenda);
        System.out.println("==========================================================================================");
        System.out.printf("%-4s | %-25s | %-12s | %-12s | %-12s%n", "ITEM", "PRODUTO", "COMPRADOS", "JÁ DEVOLVIDOS", "DISPONÍVEIS");
        System.out.println("------------------------------------------------------------------------------------------");

        for (ItemVenda item : venda.getItensVenda()) {
            String nome = item.getNomeProduto();
            if (nome.length() > 25) {
                nome = nome.substring(0, 23) + "..";
            }
            System.out.printf("%02d   | %-25s | %8d un | %10d un | %8d un%n",
                    item.getNumeroItem(),
                    nome,
                    item.getQuantidade(),
                    item.getQuantidadeDevolvida(),
                    item.getQuantidadeDisponivelParaDevolucao()
            );
        }
        System.out.println("==========================================================================================");

        int numItem;
        if (argumentos.length > 1) {
            try {
                numItem = Integer.parseInt(argumentos[1].trim());
            } catch (NumberFormatException e) {
                System.out.println("Número do item inválido.");
                return true;
            }
        } else {
            System.out.print("Informe o NÚMERO DO ITEM a devolver (ou 0 para cancelar): ");
            String inputNum = sessao.getEntrada().nextLine().trim();
            try {
                numItem = Integer.parseInt(inputNum);
            } catch (NumberFormatException e) {
                System.out.println("Número inválido.");
                return true;
            }
        }
        if (numItem <= 0) {
            System.out.println("Devolução cancelada.");
            return true;
        }

        ItemVenda itemEscolhido = venda.buscarItemPorNumero(numItem);
        if (itemEscolhido == null) {
            System.out.printf("[ERRO] Item número %d não existe nesta venda.%n", numItem);
            return true;
        }

        if (itemEscolhido.getQuantidadeDisponivelParaDevolucao() <= 0) {
            System.out.println("[ERRO] Todas as unidades deste item já foram devolvidas anteriormente.");
            return true;
        }

        int quantidade;
        if (argumentos.length > 2) {
            try {
                quantidade = Integer.parseInt(argumentos[2].trim());
            } catch (NumberFormatException e) {
                System.out.println("Quantidade informada inválida.");
                return true;
            }
        } else {
            System.out.printf("Informe a QUANTIDADE a devolver (Máximo: %d un): ", itemEscolhido.getQuantidadeDisponivelParaDevolucao());
            String inputQtd = sessao.getEntrada().nextLine().trim();
            try {
                quantidade = Integer.parseInt(inputQtd);
            } catch (NumberFormatException e) {
                System.out.println("Quantidade inválida.");
                return true;
            }
        }

        if (quantidade <= 0 || quantidade > itemEscolhido.getQuantidadeDisponivelParaDevolucao()) {
            System.out.println("[ERRO] Quantidade informada excede o limite disponível ou é menor que 1.");
            return true;
        }

        String motivo;
        if (argumentos.length > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < argumentos.length; i++) {
                if (i > 3) sb.append(" ");
                sb.append(argumentos[i]);
            }
            motivo = sb.toString();
        } else {
            System.out.print("Informe o MOTIVO da devolução: ");
            motivo = sessao.getEntrada().nextLine().trim();
        }
        if (motivo == null || motivo.isEmpty()) {
            System.out.println("O motivo da devolução é obrigatório.");
            return true;
        }

        // Buscar cliente do orçamento vinculado
        Integer idCliente = null;
        Orcamento orc = sessao.getOrcamentoDAO().buscarPorId(venda.getIdOrcamento());
        if (orc != null && orc.temCliente()) {
            idCliente = orc.getCliente().getIdCliente();
        }

        DevolucaoItem devolucao = sessao.getPosVendaDAO().processarDevolucaoItem(
                idVenda,
                itemEscolhido.getIdItemVenda(),
                quantidade,
                motivo,
                true,
                idCliente
        );

        if (devolucao != null) {
            System.out.println("\n====================================================");
            System.out.println("           DEVOLUÇÃO PROCESSADA COM SUCESSO!        ");
            System.out.println("====================================================");
            System.out.printf("ITEM DEVOLVIDO:   %s (x%d)%n", itemEscolhido.getNomeProduto(), quantidade);
            System.out.printf("VALOR REEMBOLSADO:R$ %.2f (preço líquido unitário: R$ %.2f)%n",
                    devolucao.getValorTotal(), devolucao.getValorUnitario());
            System.out.printf("MOTIVO:           %s%n", devolucao.getMotivo());
            System.out.println("====================================================\n");

            if (devolucao.getIdValeCompra() != null) {
                ValeCompra vale = sessao.getPosVendaDAO().buscarValePorId(devolucao.getIdValeCompra());
                if (vale != null) {
                    System.out.println(vale.gerarComprovante());
                }
            }
        } else {
            System.out.println("[ERRO] Ocorreu uma falha ao registrar a devolução no sistema.");
        }

        return true;
    }
}
