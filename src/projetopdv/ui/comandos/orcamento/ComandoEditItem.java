package projetopdv.ui.comandos.orcamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Scanner;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoEditItem extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;
    private final Scanner entrada;

    public ComandoEditItem(SessaoOrcamento sessao) {
        super(
                "/edit_item",
                """
                Altera quantidade ou aplica descontos/preço em um item do orçamento ativo.

                Usos:
                /edit_item                                                     (Modo interativo com menu de opções)
                /edit_item <Nº Item>                                           (Abre o menu de opções para o item)
                /edit_item <Nº Item> <qtd | valor | desc_pct | desc_val> <Novo Valor>  (Edição direta)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
        this.entrada = sessao.getEntrada();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento.");
            return true;
        }

        Orcamento orc = sessao.getOrcamentoAtivo();
        if (orc == null) {
            System.out.println("Orçamento ativo inválido.");
            return true;
        }

        if (orc.getItensOrcamento().isEmpty()) {
            System.out.println("Nenhum item cadastrado neste orçamento para edição.");
            return true;
        }

        // 1. Determinar o número do item
        int numeroItem;
        if (argumentos.length >= 1) {
            try {
                numeroItem = Integer.parseInt(argumentos[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Número do item inválido: " + argumentos[0]);
                return true;
            }
        } else {
            System.out.print("Informe o número do item que deseja editar (ou 'cancelar'): ");
            String inputItem = entrada.nextLine().trim();
            if (inputItem.equalsIgnoreCase("cancelar") || inputItem.isEmpty()) {
                System.out.println("Edição cancelada.");
                return true;
            }
            try {
                numeroItem = Integer.parseInt(inputItem);
            } catch (NumberFormatException e) {
                System.out.println("Número do item inválido.");
                return true;
            }
        }

        ItemOrcamento item = orc.buscarItemPorNumero(numeroItem);
        if (item == null) {
            System.out.println("Item #" + numeroItem + " não encontrado neste orçamento.");
            return true;
        }

        // 2. Determinar a operação e o novo valor
        String operacao;
        String valorStr;

        if (argumentos.length >= 3) {
            // Sintaxe direta em uma linha: /edit_item 1 qtd 2
            operacao = normalizarOperacao(argumentos[1].trim());
            if (operacao == null) {
                System.out.println("Operação inválida. Use: qtd, valor, desc_pct ou desc_val.");
                return true;
            }
            valorStr = argumentos[2].trim().replace(",", ".");
        } else if (argumentos.length == 2) {
            // Informou item e operação: /edit_item 1 qtd
            operacao = normalizarOperacao(argumentos[1].trim());
            if (operacao == null) {
                System.out.println("Operação inválida. Use: [1] qtd, [2] valor, [3] desc_pct, [4] desc_val");
                return true;
            }
            valorStr = solicitarValorOperacao(operacao);
            if (valorStr == null) {
                System.out.println("Edição cancelada.");
                return true;
            }
        } else {
            // Menu de opções interativo: /edit_item ou /edit_item 1
            System.out.println(String.format("\n--- EDITAR ITEM %02d: %s ---", item.getNumeroItem(), item.getNomeProduto()));
            System.out.println("[1] Editar quantidade");
            System.out.println("[2] Editar valor unitário");
            System.out.println("[3] Aplicar desconto percentual (%)");
            System.out.println("[4] Aplicar desconto em valor (R$)");
            System.out.print("Escolha uma opção (1-4 ou 'cancelar'): ");

            String opcaoEscolhida = entrada.nextLine().trim();
            if (opcaoEscolhida.equalsIgnoreCase("cancelar") || opcaoEscolhida.isEmpty()) {
                System.out.println("Edição cancelada.");
                return true;
            }

            operacao = normalizarOperacao(opcaoEscolhida);
            if (operacao == null) {
                System.out.println("Opção inválida.");
                return true;
            }

            valorStr = solicitarValorOperacao(operacao);
            if (valorStr == null) {
                System.out.println("Edição cancelada.");
                return true;
            }
        }

        // 3. Executar a alteração no banco e atualizar o orçamento
        try {
            switch (operacao) {
                case "qtd" -> {
                    if (!sessao.validarPermissao(Permissao.ORCAMENTO_EDITAR_ITENS)) {
                        return true;
                    }

                    if (orc.getStatusOrcamento() != StatusOrcamento.ABERTO) {
                        System.out.println("A quantidade só pode ser alterada quando o orçamento estiver ABERTO.");
                        return true;
                    }

                    int novaQtd = Integer.parseInt(valorStr);
                    if (novaQtd <= 0) {
                        System.out.println("A quantidade deve ser maior que zero.");
                        return true;
                    }

                    int qtdAnterior = item.getQuantidade();
                    orcamentoDAO.alterarQuantidadeItem(orc.getIdOrcamento(), numeroItem, novaQtd);
                    Orcamento orcAtualizado = sessao.getOrcamentoAtivo();
                    ItemOrcamento itemAtualizado = orcAtualizado.buscarItemPorNumero(numeroItem);

                    System.out.println("\n[QUANTIDADE DO ITEM ALTERADA]");
                    System.out.println(String.format("Item %02d: %s - Quantidade anterior: %d -> Nova quantidade: %d",
                            numeroItem, item.getNomeProduto(), qtdAnterior, novaQtd));
                    System.out.println(String.format("Subtotal do Item: R$ %.2f | Novo Total do Orçamento: R$ %.2f",
                            itemAtualizado.getValorItem(), orcAtualizado.getValorTotal()));
                }

                case "valor" -> {
                    if (!sessao.validarPermissao(Permissao.ORCAMENTO_APLICAR_DESCONTO)) {
                        return true;
                    }

                    BigDecimal novoPreco = new BigDecimal(valorStr);
                    if (novoPreco.compareTo(BigDecimal.ZERO) < 0) {
                        System.out.println("O preço unitário não pode ser negativo.");
                        return true;
                    }

                    BigDecimal precoAnterior = item.getPrecoUnitarioLiquido();
                    orcamentoDAO.aplicarDescontoItem(orc.getIdOrcamento(), numeroItem, novoPreco);
                    Orcamento orcAtualizado = sessao.getOrcamentoAtivo();
                    ItemOrcamento itemAtualizado = orcAtualizado.buscarItemPorNumero(numeroItem);

                    System.out.println("\n[VALOR UNITÁRIO DO ITEM ALTERADO]");
                    System.out.println(String.format("Item %02d: %s - Tabela: R$ %.2f | Preço anterior: R$ %.2f -> Novo líquido: R$ %.2f (Desconto: R$ %.2f / %.2f%%)",
                            numeroItem, item.getNomeProduto(), item.getPrecoUnitarioTabela(), precoAnterior, novoPreco, itemAtualizado.getValorDescontoUnitario(), itemAtualizado.getPercentualDesconto()));
                    System.out.println(String.format("Subtotal do Item: R$ %.2f | Novo Total do Orçamento: R$ %.2f",
                            itemAtualizado.getValorItem(), orcAtualizado.getValorTotal()));
                }

                case "desc_pct" -> {
                    if (!sessao.validarPermissao(Permissao.ORCAMENTO_APLICAR_DESCONTO)) {
                        return true;
                    }

                    BigDecimal pct = new BigDecimal(valorStr);
                    if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
                        System.out.println("Percentual de desconto deve estar entre 0 e 100.");
                        return true;
                    }

                    BigDecimal fator = BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                    BigDecimal novoPreco = item.getPrecoUnitarioTabela().multiply(fator).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal precoAnterior = item.getPrecoUnitarioLiquido();

                    orcamentoDAO.aplicarDescontoItem(orc.getIdOrcamento(), numeroItem, novoPreco);
                    Orcamento orcAtualizado = sessao.getOrcamentoAtivo();
                    ItemOrcamento itemAtualizado = orcAtualizado.buscarItemPorNumero(numeroItem);

                    System.out.println("\n[DESCONTO PERCENTUAL APLICADO NO ITEM]");
                    System.out.println(String.format("Item %02d: %s - Tabela: R$ %.2f | Preço anterior: R$ %.2f -> Novo líquido: R$ %.2f (%.2f%% OFF / -R$ %.2f)",
                            numeroItem, item.getNomeProduto(), item.getPrecoUnitarioTabela(), precoAnterior, novoPreco, pct, itemAtualizado.getValorDescontoUnitario()));
                    System.out.println(String.format("Subtotal do Item: R$ %.2f | Novo Total do Orçamento: R$ %.2f",
                            itemAtualizado.getValorItem(), orcAtualizado.getValorTotal()));
                }

                case "desc_val" -> {
                    if (!sessao.validarPermissao(Permissao.ORCAMENTO_APLICAR_DESCONTO)) {
                        return true;
                    }

                    BigDecimal descVal = new BigDecimal(valorStr);
                    if (descVal.compareTo(BigDecimal.ZERO) < 0) {
                        System.out.println("O valor de desconto não pode ser negativo.");
                        return true;
                    }
                    if (descVal.compareTo(item.getPrecoUnitarioTabela()) > 0) {
                        System.out.println("O desconto (R$ " + descVal + ") não pode ser maior que o preço unitário de tabela (R$ " + item.getPrecoUnitarioTabela() + ").");
                        return true;
                    }

                    BigDecimal novoPreco = item.getPrecoUnitarioTabela().subtract(descVal).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal precoAnterior = item.getPrecoUnitarioLiquido();

                    orcamentoDAO.aplicarDescontoItem(orc.getIdOrcamento(), numeroItem, novoPreco);
                    Orcamento orcAtualizado = sessao.getOrcamentoAtivo();
                    ItemOrcamento itemAtualizado = orcAtualizado.buscarItemPorNumero(numeroItem);

                    System.out.println("\n[DESCONTO EM VALOR APLICADO NO ITEM]");
                    System.out.println(String.format("Item %02d: %s - Tabela: R$ %.2f | Preço anterior: R$ %.2f -> Novo líquido: R$ %.2f (-R$ %.2f / %.2f%% OFF)",
                            numeroItem, item.getNomeProduto(), item.getPrecoUnitarioTabela(), precoAnterior, novoPreco, descVal, itemAtualizado.getPercentualDesconto()));
                    System.out.println(String.format("Subtotal do Item: R$ %.2f | Novo Total do Orçamento: R$ %.2f",
                            itemAtualizado.getValorItem(), orcAtualizado.getValorTotal()));
                }

                default -> System.out.println("Operação inválida. Use: qtd, valor, desc_pct ou desc_val.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Valor numérico inválido: " + valorStr);
        } catch (SQLException e) {
            System.out.println("Erro ao atualizar item no banco: " + e.getMessage());
        }

        return true;
    }

    private String normalizarOperacao(String op) {
        String lower = op.toLowerCase().trim();
        return switch (lower) {
            case "1", "qtd", "quantidade" -> "qtd";
            case "2", "valor", "preco", "preco_unitario" -> "valor";
            case "3", "desc_pct", "desconto_pct", "pct", "%" -> "desc_pct";
            case "4", "desc_val", "desc_valor", "desconto_val", "desconto_valor", "val", "r$" -> "desc_val";
            default -> null;
        };
    }

    private String solicitarValorOperacao(String operacao) {
        String prompt = switch (operacao) {
            case "qtd" -> "Escreva a quantidade desejada: ";
            case "valor" -> "Escreva o novo valor unitário: R$ ";
            case "desc_pct" -> "Escreva o percentual de desconto (%): ";
            case "desc_val" -> "Escreva o valor de desconto no unitário: R$ ";
            default -> "Informe o novo valor: ";
        };

        System.out.print(prompt);
        String input = entrada.nextLine().trim();
        if (input.equalsIgnoreCase("cancelar") || input.isEmpty()) {
            return null;
        }
        return input.replace(",", ".");
    }
}

