package projetopdv.ui.comandos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoProduto extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoProduto(SessaoOrcamento sessao) {
        super(
                "/produto",
                """
                Informa ou altera os dados de um produto através do ID ou painel interativo.

                Usos:
                /produto                                            (Modo interativo com busca/listagem e menu de opções)
                /produto <ID do Produto>                            (Abre o painel interativo de opções para o produto)
                /produto <ID do Produto> <Subcomando> [Argumentos]  (Edição direta via comando)

                Argumentos de Consulta:
                    nome            Nome do produto
                    preco           Preço do produto
                    cod_barras      Código de barras do produto

                Argumentos de Edição:
                    set_nome        Define um novo nome para o produto
                    set_preco       Define um novo preço para o produto
                    set_cod_barras  Define um novo código de barras

                Argumentos de Exclusão:
                    deletar         Deleta o produto
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        ProdutoDAO produtoDAO = sessao.getProdutoDAO();

        // ==========================================
        // CASO 1: SEM ARGUMENTOS (/produto)
        // ==========================================
        if (argumentos.length == 0) {
            if (!sessao.validarPermissao(Permissao.PRODUTO_CONSULTAR)) {
                return true;
            }

            Scanner entrada = sessao.getEntrada();
            System.out.print("Informe o ID, Código de Barras ou Nome do produto (ou 'listar' / 'cancelar'): ");
            String busca = entrada.nextLine().trim();

            if (busca.isEmpty() || busca.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação cancelada.");
                return true;
            }

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
                        System.out.printf("[%d] ID: %-4d | %-30s | R$ %8.2f | Cód. Barras: %s%n",
                                (i + 1), p.getIdProduto(), p.getNomeProduto(), p.getPrecoVenda(),
                                p.getCodBarras().isEmpty() ? "Não informado" : p.getCodBarras());
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
                            return gerenciarProdutoInterativo(todos.get(num - 1));
                        }
                        Produto porId = produtoDAO.buscarPorId(num);
                        if (porId != null) {
                            return gerenciarProdutoInterativo(porId);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    System.out.println("Opção inválida.");
                    return true;
                } catch (SQLException e) {
                    System.out.println("Erro ao listar produtos: " + e.getMessage());
                    return true;
                }
            }

            // Tenta localizar por ID
            try {
                int id = Integer.parseInt(busca);
                Produto p = produtoDAO.buscarPorId(id);
                if (p != null) {
                    return gerenciarProdutoInterativo(p);
                }
            } catch (NumberFormatException ignored) {
            }

            // Tenta por código de barras
            Produto pBarras = produtoDAO.buscarPorCodBarras(busca);
            if (pBarras != null) {
                return gerenciarProdutoInterativo(pBarras);
            }

            // Tenta por termo de pesquisa
            List<Produto> encontrados = produtoDAO.pesquisarPorTermo(busca);
            if (encontrados.isEmpty()) {
                System.out.println("Nenhum produto encontrado com o termo '" + busca + "'.");
                return true;
            }

            if (encontrados.size() == 1) {
                return gerenciarProdutoInterativo(encontrados.get(0));
            }

            System.out.println("\nForam encontrados " + encontrados.size() + " produtos para '" + busca + "':");
            for (int i = 0; i < encontrados.size(); i++) {
                Produto p = encontrados.get(i);
                System.out.printf("[%d] ID: %-4d | %-30s | R$ %8.2f%n",
                        (i + 1), p.getIdProduto(), p.getNomeProduto(), p.getPrecoVenda());
            }
            System.out.print("Escolha o número do produto desejado (ou 'cancelar'): ");
            String esc = entrada.nextLine().trim();
            if (esc.equalsIgnoreCase("cancelar") || esc.isEmpty()) {
                System.out.println("Operação cancelada.");
                return true;
            }
            try {
                int op = Integer.parseInt(esc);
                if (op >= 1 && op <= encontrados.size()) {
                    return gerenciarProdutoInterativo(encontrados.get(op - 1));
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.println("Opção inválida.");
            return true;
        }

        // ==========================================
        // CASO 2: COM ARGUMENTOS
        // ==========================================
        int idProduto;
        try {
            idProduto = Integer.parseInt(argumentos[0]);
        } catch (NumberFormatException e) {
            System.out.println("ID do produto inválido.");
            return true;
        }

        Produto produto = produtoDAO.buscarPorId(idProduto);
        if (produto == null) {
            System.out.println("Produto não encontrado.");
            return true;
        }

        // Se informou apenas o ID (/produto <ID>), abre o painel interativo do produto
        if (argumentos.length == 1) {
            return gerenciarProdutoInterativo(produto);
        }

        String arg = argumentos[1].toLowerCase();

        switch (arg) {
            // =========================
            // CONSULTAS
            // =========================
            case "nome" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_CONSULTAR)) return true;
                System.out.println(produto.getNomeProduto());
            }

            case "preco" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_CONSULTAR)) return true;
                System.out.println(String.format("R$ %.2f", produto.getPrecoVenda()));
            }

            case "cod_barras" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_CONSULTAR)) return true;
                System.out.println(produto.getCodBarras().isEmpty() ? "Não informado" : produto.getCodBarras());
            }

            // =========================
            // ALTERAR NOME
            // =========================
            case "set_nome" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EDITAR)) {
                    return true;
                }

                if (argumentos.length < 3) {
                    System.out.println("Uso: /produto <ID> set_nome <Novo Nome>");
                    return true;
                }

                String novoNome = String.join(" ", java.util.Arrays.copyOfRange(argumentos, 2, argumentos.length)).trim();

                try {
                    produto.setNomeProduto(novoNome);
                    if (produtoDAO.atualizar(produto)) {
                        System.out.println("Nome alterado com sucesso.");
                    } else {
                        System.out.println("Não foi possível alterar o nome.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar nome: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar produto: " + e.getMessage());
                }
            }

            // =========================
            // ALTERAR PREÇO
            // =========================
            case "set_preco" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EDITAR)) {
                    return true;
                }

                if (argumentos.length != 3) {
                    System.out.println("Uso: /produto <ID> set_preco <Novo Preço>");
                    return true;
                }

                try {
                    String novoPrecoStr = argumentos[2].replace(",", ".");
                    BigDecimal novoPreco = new BigDecimal(novoPrecoStr);

                    produto.setPrecoVenda(novoPreco);

                    if (produtoDAO.atualizar(produto)) {
                        System.out.println("Preço alterado com sucesso.");
                    } else {
                        System.out.println("Não foi possível alterar o preço.");
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Preço inválido.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar preço: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar produto: " + e.getMessage());
                }
            }

            // =========================
            // ALTERAR CÓDIGO DE BARRAS
            // =========================
            case "set_cod_barras" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EDITAR)) {
                    return true;
                }

                if (argumentos.length != 3) {
                    System.out.println("Uso: /produto <ID> set_cod_barras <Novo Código de Barras>");
                    return true;
                }

                String novoCodBarras = argumentos[2];

                try {
                    produto.setCodBarras(novoCodBarras);

                    if (produtoDAO.atualizar(produto)) {
                        System.out.println("Código de barras alterado com sucesso.");
                    } else {
                        System.out.println("Não foi possível alterar o código de barras.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar código de barras: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar produto: " + e.getMessage());
                }
            }

            // =========================
            // DELETAR
            // =========================
            case "deletar" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EXCLUIR)) {
                    return true;
                }

                if (argumentos.length != 2) {
                    System.out.println("Uso: /produto <ID> deletar");
                    return true;
                }

                try {
                    if (produtoDAO.deletar(idProduto)) {
                        System.out.println("Produto deletado com sucesso.");
                    } else {
                        System.out.println("Produto não encontrado.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
                }
            }

            // =========================
            // INVÁLIDO
            // =========================
            default -> System.out.println("Argumento inválido. Digite /help /produto para ver as opções.");
        }

        return true;
    }

    private boolean gerenciarProdutoInterativo(Produto produto) {
        if (!sessao.validarPermissao(Permissao.PRODUTO_CONSULTAR)) {
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        ProdutoDAO produtoDAO = sessao.getProdutoDAO();

        System.out.println("\n" + produto);
        System.out.println("==================================================");
        System.out.println("           PAINEL DE GESTÃO DO PRODUTO            ");
        System.out.println("==================================================");
        System.out.printf("Produto: #%d - %s%n", produto.getIdProduto(), produto.getNomeProduto());
        System.out.printf("Preço: R$ %.2f | Cód. Barras: %s%n", produto.getPrecoVenda(),
                produto.getCodBarras().isEmpty() ? "Não informado" : produto.getCodBarras());
        System.out.println("--------------------------------------------------");
        System.out.println("[1] Consultar dados detalhados");
        System.out.println("[2] Alterar nome");
        System.out.println("[3] Alterar preço de venda");
        System.out.println("[4] Alterar código de barras");
        System.out.println("[5] Deletar produto");
        System.out.println("[0] Concluir / Cancelar");
        System.out.println("--------------------------------------------------");
        System.out.print("Escolha uma opção (0-5 ou 'cancelar'): ");

        String opcao = entrada.nextLine().trim();
        if (opcao.equalsIgnoreCase("cancelar") || opcao.equals("0") || opcao.isEmpty()) {
            System.out.println("Operação concluída.");
            return true;
        }

        switch (opcao) {
            case "1" -> {
                System.out.println("\n" + produto);
            }
            case "2" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EDITAR)) return true;
                System.out.printf("Nome atual: %s%n", produto.getNomeProduto());
                System.out.print("Informe o novo nome (ou 'cancelar'): ");
                String novoNome = entrada.nextLine().trim();
                if (novoNome.isEmpty() || novoNome.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    produto.setNomeProduto(novoNome);
                    if (produtoDAO.atualizar(produto)) {
                        System.out.println("✅ Nome alterado com sucesso para: " + novoNome);
                    } else {
                        System.out.println("Não foi possível alterar o nome.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar nome: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar produto: " + e.getMessage());
                }
            }
            case "3" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EDITAR)) return true;
                System.out.printf("Preço atual: R$ %.2f%n", produto.getPrecoVenda());
                System.out.print("Informe o novo preço de venda (R$) (ou 'cancelar'): ");
                String novoPrecoStr = entrada.nextLine().trim().replace(",", ".");
                if (novoPrecoStr.isEmpty() || novoPrecoStr.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    BigDecimal novoPreco = new BigDecimal(novoPrecoStr);
                    produto.setPrecoVenda(novoPreco);
                    if (produtoDAO.atualizar(produto)) {
                        System.out.printf("✅ Preço alterado com sucesso para: R$ %.2f%n", novoPreco);
                    } else {
                        System.out.println("Não foi possível alterar o preço.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Preço inválido.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar preço: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar produto: " + e.getMessage());
                }
            }
            case "4" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EDITAR)) return true;
                System.out.printf("Código de barras atual: %s%n", produto.getCodBarras().isEmpty() ? "Não informado" : produto.getCodBarras());
                System.out.print("Informe o novo código de barras (ou 'cancelar'): ");
                String novoCodBarras = entrada.nextLine().trim();
                if (novoCodBarras.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    produto.setCodBarras(novoCodBarras);
                    if (produtoDAO.atualizar(produto)) {
                        System.out.println("✅ Código de barras alterado com sucesso!");
                    } else {
                        System.out.println("Não foi possível alterar o código de barras.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar código de barras: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar produto: " + e.getMessage());
                }
            }
            case "5" -> {
                if (!sessao.validarPermissao(Permissao.PRODUTO_EXCLUIR)) return true;
                System.out.printf("⚠️ Tem certeza que deseja deletar o produto #%d - %s permanentemente? (s/n): ",
                        produto.getIdProduto(), produto.getNomeProduto());
                String conf = entrada.nextLine().trim().toLowerCase();
                if (!conf.equals("s") && !conf.equals("sim")) {
                    System.out.println("Exclusão cancelada.");
                    return true;
                }
                try {
                    if (produtoDAO.deletar(produto.getIdProduto())) {
                        System.out.println("✅ Produto deletado com sucesso.");
                    } else {
                        System.out.println("Não foi possível deletar o produto.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao excluir produto no banco: " + e.getMessage());
                }
            }
            default -> System.out.println("Opção inválida.");
        }

        return true;
    }
}
