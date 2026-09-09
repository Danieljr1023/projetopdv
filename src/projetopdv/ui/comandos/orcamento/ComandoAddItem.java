package projetopdv.ui.comandos.orcamento;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoAddItem extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final ProdutoDAO produtoDAO;

    public ComandoAddItem(SessaoOrcamento sessao) {
        super(
                "/add",
                """
                Adiciona um item ao orçamento ativo por ID, Código de Barras, Busca por % ou modo interativo.

                Usos:
                /add                                               (Modo interativo com busca/listagem, quantidade e preço)
                /add <ID ou Código de Barras> [Quantidade] [Preço] (Adição direta)
                /add %<Termo de Busca> [Quantidade]                (Busca por nome/descrição)
                """
        );
        this.sessao = sessao;
        this.produtoDAO = sessao.getProdutoDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_EDITAR_ITENS)) {
            return true;
        }

        if (!sessao.temOrcamentoAtivo()) {
            System.out.println("Nenhum orçamento ativo no momento. Use /novo_orcamento ou /abrir primeiro.");
            return true;
        }

        Orcamento orc = sessao.getOrcamentoAtivo();
        if (orc == null || orc.getStatusOrcamento() != StatusOrcamento.ABERTO) {
            System.out.println("Não é possível adicionar itens em um orçamento com status " + (orc != null ? orc.getStatusOrcamento() : "inválido") + ".");
            return true;
        }

        if (argumentos.length < 1) {
            Scanner entrada = sessao.getEntrada();
            System.out.println("\n--- ADICIONAR ITEM AO ORÇAMENTO ---");
            System.out.print("Informe o ID, Código de Barras ou Nome do produto (ou 'listar' / 'cancelar'): ");
            String busca = entrada.nextLine().trim();

            if (busca.isEmpty() || busca.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação cancelada.");
                return true;
            }

            Produto produtoSelecionado = null;

            if (busca.equalsIgnoreCase("listar")) {
                try {
                    List<Produto> todos = produtoDAO.listarTodos();
                    if (todos.isEmpty()) {
                        System.out.println("Nenhum produto cadastrado.");
                        return true;
                    }
                    System.out.println("\n=== CATÁLOGO DE PRODUTOS ===");
                    for (int i = 0; i < todos.size(); i++) {
                        Produto p = todos.get(i);
                        System.out.printf("[%d] ID: %-4d | %-30s | R$ %8.2f%n",
                                (i + 1), p.getIdProduto(), p.getNomeProduto(), p.getPrecoVenda());
                    }
                    System.out.print("\nEscolha o número da opção desejada ou informe o ID (ou 'cancelar'): ");
                    String sel = entrada.nextLine().trim();
                    if (sel.isEmpty() || sel.equalsIgnoreCase("cancelar")) {
                        System.out.println("Operação cancelada.");
                        return true;
                    }
                    try {
                        int num = Integer.parseInt(sel);
                        if (num >= 1 && num <= todos.size()) {
                            produtoSelecionado = todos.get(num - 1);
                        } else {
                            produtoSelecionado = produtoDAO.buscarPorId(num);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao listar produtos: " + e.getMessage());
                    return true;
                }
            } else {
                // Tenta por ID
                try {
                    int id = Integer.parseInt(busca);
                    produtoSelecionado = produtoDAO.buscarPorId(id);
                } catch (NumberFormatException ignored) {
                }

                // Tenta por código de barras
                if (produtoSelecionado == null) {
                    produtoSelecionado = produtoDAO.buscarPorCodBarras(busca);
                }

                // Tenta por pesquisa de termo
                if (produtoSelecionado == null) {
                    String termoLimpo = busca.replace("%", "").trim();
                    List<Produto> encontrados = produtoDAO.pesquisarPorTermo(termoLimpo);
                    if (encontrados.size() == 1) {
                        produtoSelecionado = encontrados.get(0);
                    } else if (encontrados.size() > 1) {
                        System.out.println("\nForam encontrados " + encontrados.size() + " produtos:");
                        for (int i = 0; i < encontrados.size(); i++) {
                            Produto p = encontrados.get(i);
                            System.out.printf("[%d] ID: %-4d | %-30s | R$ %8.2f%n",
                                    (i + 1), p.getIdProduto(), p.getNomeProduto(), p.getPrecoVenda());
                        }
                        System.out.print("Escolha o número da opção desejada (ou 'cancelar'): ");
                        String sel = entrada.nextLine().trim();
                        if (sel.isEmpty() || sel.equalsIgnoreCase("cancelar")) {
                            System.out.println("Operação cancelada.");
                            return true;
                        }
                        try {
                            int idx = Integer.parseInt(sel);
                            if (idx >= 1 && idx <= encontrados.size()) {
                                produtoSelecionado = encontrados.get(idx - 1);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            if (produtoSelecionado == null) {
                System.out.println("Produto não encontrado.");
                return true;
            }

            System.out.printf("Produto selecionado: %s (Preço de Tabela: R$ %.2f)%n",
                    produtoSelecionado.getNomeProduto(), produtoSelecionado.getPrecoVenda());

            // Pergunta quantidade
            System.out.print("Informe a quantidade [Pressione ENTER para 1]: ");
            String qtdStr = entrada.nextLine().trim();
            int quantidade = 1;
            if (!qtdStr.isEmpty()) {
                try {
                    quantidade = Integer.parseInt(qtdStr);
                    if (quantidade <= 0) {
                        System.out.println("A quantidade deve ser maior que zero.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Quantidade inválida.");
                    return true;
                }
            }

            // Pergunta preço unitário
            System.out.printf("Informe o preço unitário líquido [Pressione ENTER para R$ %.2f]: R$ ",
                    produtoSelecionado.getPrecoVenda());
            String precoStr = entrada.nextLine().trim().replace(",", ".");
            BigDecimal precoUnitario = produtoSelecionado.getPrecoVenda();
            if (!precoStr.isEmpty()) {
                try {
                    BigDecimal p = new BigDecimal(precoStr);
                    if (p.compareTo(BigDecimal.ZERO) < 0) {
                        System.out.println("O preço não pode ser negativo.");
                        return true;
                    }
                    precoUnitario = p;
                } catch (NumberFormatException e) {
                    System.out.println("Preço inválido.");
                    return true;
                }
            }

            sessao.adicionarProdutoDireto(produtoSelecionado, quantidade, precoUnitario);
            return true;
        }

        // Caso 1: Busca com wildcard (%)
        boolean contemWildcard = false;
        for (String arg : argumentos) {
            if (arg.contains("%")) {
                contemWildcard = true;
                break;
            }
        }

        if (contemWildcard) {
            int quantidade = 1;
            BigDecimal precoUnitario = null;
            int numTermArgs = argumentos.length;

            // Verifica se os últimos argumentos são quantidade e preço
            if (numTermArgs >= 3) {
                try {
                    String precoStr = argumentos[numTermArgs - 1].replace(",", ".");
                    BigDecimal p = new BigDecimal(precoStr);
                    int q = Integer.parseInt(argumentos[numTermArgs - 2]);
                    if (q > 0 && p.compareTo(BigDecimal.ZERO) >= 0) {
                        precoUnitario = p;
                        quantidade = q;
                        numTermArgs -= 2;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (precoUnitario == null && numTermArgs >= 2) {
                try {
                    int q = Integer.parseInt(argumentos[numTermArgs - 1]);
                    if (q > 0) {
                        quantidade = q;
                        numTermArgs -= 1;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            String[] termoPartes = new String[numTermArgs];
            System.arraycopy(argumentos, 0, termoPartes, 0, numTermArgs);
            String termo = String.join(" ", termoPartes).trim();

            List<Produto> encontrados = produtoDAO.pesquisarPorTermo(termo);
            if (encontrados.isEmpty()) {
                System.out.println("\nNenhum produto encontrado para a busca '" + termo + "'.");
                return true;
            }

            if (encontrados.size() == 1) {
                Produto p = encontrados.get(0);
                sessao.adicionarProdutoDireto(p, quantidade, precoUnitario != null ? precoUnitario : p.getPrecoVenda());
                return true;
            }

            System.out.println("\n================================================================================");
            System.out.println("                   RESULTADOS ENCONTRADOS PARA '" + termo + "'                  ");
            System.out.println("================================================================================");
            for (int i = 0; i < encontrados.size(); i++) {
                Produto p = encontrados.get(i);
                System.out.println(String.format("[%d] ID: %-4d | %-32s | Cód: %-13s | R$ %7.2f",
                        (i + 1), p.getIdProduto(), p.getNomeProduto(), p.getCodBarras(), p.getPrecoVenda()));
            }
            System.out.println("================================================================================");
            System.out.println("Digite o número da opção desejada para adicionar (ou digite outro comando):");

            sessao.definirOpcoesProdutosPendentes(encontrados, quantidade, precoUnitario);
            return true;
        }

        int argOffset = 0;
        boolean forcarId = false;
        boolean forcarCod = false;

        if (argumentos[0].equalsIgnoreCase("id") && argumentos.length > 1) {
            forcarId = true;
            argOffset = 1;
        } else if (argumentos[0].equalsIgnoreCase("cod") && argumentos.length > 1) {
            forcarCod = true;
            argOffset = 1;
        }

        String termo = argumentos[argOffset].trim();
        int quantidade = 1;
        BigDecimal precoUnitario = null;

        if (argumentos.length >= argOffset + 2) {
            try {
                quantidade = Integer.parseInt(argumentos[argOffset + 1].trim());
                if (quantidade <= 0) {
                    System.out.println("A quantidade deve ser maior que zero.");
                    return true;
                }
            } catch (NumberFormatException e) {
                System.out.println("Quantidade inválida.");
                return true;
            }
        }

        if (argumentos.length >= argOffset + 3) {
            try {
                String precoStr = argumentos[argOffset + 2].trim().replace(",", ".");
                precoUnitario = new BigDecimal(precoStr);
                if (precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("O preço unitário não pode ser negativo.");
                    return true;
                }
            } catch (NumberFormatException e) {
                System.out.println("Preço unitário inválido.");
                return true;
            }
        }

        // Caso 2: Busca direta por ID ou Código de Barras
        List<Produto> candidatos = new ArrayList<>();

        // Tenta ID se não estiver forçando código de barras
        if (!forcarCod) {
            try {
                int id = Integer.parseInt(termo);
                Produto pId = produtoDAO.buscarPorId(id);
                if (pId != null) {
                    candidatos.add(pId);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Tenta Código de Barras se não estiver forçando ID
        if (!forcarId) {
            Produto pCod = produtoDAO.buscarPorCodBarras(termo);
            if (pCod != null && (candidatos.isEmpty() || candidatos.get(0).getIdProduto() != pCod.getIdProduto())) {
                candidatos.add(pCod);
            }
        }

        // Se encontrou exatamente 1
        if (candidatos.size() == 1) {
            Produto p = candidatos.get(0);
            sessao.adicionarProdutoDireto(p, quantidade, precoUnitario != null ? precoUnitario : p.getPrecoVenda());
            return true;
        }

        // Se houve conflito (ex: termo casa com ID de um e Código de Barras de outro)
        if (candidatos.size() > 1) {
            System.out.println("\nForam encontradas 2 correspondências para '" + termo + "':");
            System.out.println(String.format("[1] [Por ID]          ID: %-4d | %-30s | Cód: %-13s | R$ %.2f",
                    candidatos.get(0).getIdProduto(), candidatos.get(0).getNomeProduto(), candidatos.get(0).getCodBarras(), candidatos.get(0).getPrecoVenda()));
            System.out.println(String.format("[2] [Por Cód. Barras] ID: %-4d | %-30s | Cód: %-13s | R$ %.2f",
                    candidatos.get(1).getIdProduto(), candidatos.get(1).getNomeProduto(), candidatos.get(1).getCodBarras(), candidatos.get(1).getPrecoVenda()));
            System.out.println("Digite o número da opção desejada para adicionar:");

            sessao.definirOpcoesProdutosPendentes(candidatos, quantidade, precoUnitario);
            return true;
        }

        System.out.println("Produto '" + termo + "' não encontrado por ID ou Código de Barras.");
        return true;
    }
}
