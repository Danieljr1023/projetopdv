package projetopdv.ui.comandos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import projetopdv.orcamento.ItemOrcamento;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.produto.Produto;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoDuplicarOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;
    private final ProdutoDAO produtoDAO;
    private final Scanner entrada;

    public ComandoDuplicarOrcamento(SessaoOrcamento sessao) {
        super(
                "/duplicar",
                """
                Clona os itens de qualquer orçamento (ABERTO, CONFIRMADO, CANCELADO, FATURANDO, FINALIZADO)
                gerando um novo orçamento com status ABERTO.
                Analisa divergências com o catálogo de produtos e solicita confirmação antes de clonar.

                Usos:
                /duplicar <ID ou Nome>
                /duplicar               (Duplica o orçamento ativo ou solicita ID/Nome)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
        this.produtoDAO = sessao.getProdutoDAO();
        this.entrada = sessao.getEntrada();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_DUPLICAR)) {
            return true;
        }

        try {
            Orcamento origem = null;

            if (argumentos.length > 0) {
                String termo = String.join(" ", argumentos).trim();
                if (termo.toLowerCase().startsWith("id ")) {
                    termo = termo.substring(3).trim();
                } else if (termo.toLowerCase().startsWith("nome ")) {
                    termo = termo.substring(5).trim();
                }

                // Tenta buscar por ID
                try {
                    int id = Integer.parseInt(termo);
                    origem = orcamentoDAO.buscarPorId(id);
                } catch (NumberFormatException ignored) {
                }

                // Se não achou por ID, tenta por Nome
                if (origem == null) {
                    List<Orcamento> encontrados = orcamentoDAO.buscarPorNome(termo);
                    if (encontrados.size() == 1) {
                        origem = encontrados.get(0);
                    } else if (encontrados.size() > 1) {
                        System.out.println("\nForam encontrados múltiplos orçamentos para '" + termo + "':");
                        for (int i = 0; i < encontrados.size(); i++) {
                            Orcamento o = encontrados.get(i);
                            String nome;
                            try {
                                nome = o.getNomeOrcamento();
                            } catch (Exception e) {
                                nome = "Sem Nome";
                            }
                            System.out.println(String.format("[%d] ID: %-4d | Nome: %-25s | Status: %-10s | Total: R$ %9.2f",
                                    (i + 1), o.getIdOrcamento(), nome, o.getStatusOrcamento(), o.getValorTotal()));
                        }
                        System.out.print("Digite o número da opção desejada para duplicar (ou 'cancelar'): ");
                        String opStr = entrada.nextLine().trim();
                        if (opStr.equalsIgnoreCase("cancelar")) {
                            System.out.println("Duplicação cancelada.");
                            return true;
                        }
                        try {
                            int idx = Integer.parseInt(opStr);
                            if (idx >= 1 && idx <= encontrados.size()) {
                                origem = encontrados.get(idx - 1);
                            } else {
                                System.out.println("Opção inválida.");
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Entrada inválida.");
                            return true;
                        }
                    }
                }
            } else if (sessao.temOrcamentoAtivo()) {
                origem = sessao.getOrcamentoAtivo();
            } else {
                System.out.print("Digite o ID ou Nome do orçamento a ser duplicado (ou 'cancelar'): ");
                String input = entrada.nextLine().trim();
                if (input.equalsIgnoreCase("cancelar") || input.isEmpty()) {
                    System.out.println("Duplicação cancelada.");
                    return true;
                }
                return executar(new String[]{input});
            }

            if (origem == null) {
                System.out.println("Orçamento de origem não encontrado.");
                return true;
            }

            // 1. Exibir resumo detalhado do orçamento que será clonado
            System.out.println("\n--- ORÇAMENTO SELECIONADO PARA DUPLICAÇÃO ---");
            sessao.imprimirResumoCompleto(origem);

            // 2. Analisar divergências entre os itens do orçamento e o catálogo atual
            List<String> divergencias = new ArrayList<>();
            for (ItemOrcamento item : origem.getItensOrcamento()) {
                if (item.getIdProduto() > 0) {
                    Produto prodAtual = produtoDAO.buscarPorId(item.getIdProduto());
                    if (prodAtual == null) {
                        divergencias.add(String.format("• Item %02d: Produto #%d foi EXCLUÍDO do cadastro de produtos.",
                                item.getNumeroItem(), item.getIdProduto()));
                    } else {
                        StringBuilder diff = new StringBuilder();
                        if (!prodAtual.getNomeProduto().equals(item.getNomeProduto())) {
                            diff.append(String.format("\n    - Nome no Catálogo: '%s' | No Orçamento: '%s'",
                                    prodAtual.getNomeProduto(), item.getNomeProduto()));
                        }
                        if (!prodAtual.getCodBarras().equals(item.getCodigoBarras())) {
                            diff.append(String.format("\n    - Cód. Barras no Catálogo: '%s' | No Orçamento: '%s'",
                                    prodAtual.getCodBarras(), item.getCodigoBarras()));
                        }
                        if (prodAtual.getPrecoVenda().compareTo(item.getPrecoUnitarioTabela()) != 0) {
                            BigDecimal difPreco = prodAtual.getPrecoVenda().subtract(item.getPrecoUnitarioTabela());
                            String sinal = difPreco.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
                            diff.append(String.format("\n    - Preço no Catálogo: R$ %.2f | Tabela no Orçamento: R$ %.2f (%sR$ %.2f)",
                                    prodAtual.getPrecoVenda(), item.getPrecoUnitarioTabela(), sinal, difPreco));
                        }
                        if (diff.length() > 0) {
                            divergencias.add(String.format("• Item %02d (%s | ID: %d):%s",
                                    item.getNumeroItem(), item.getNomeProduto(), item.getIdProduto(), diff.toString()));
                        }
                    }
                }
            }

            // 3. Exibir divergências se existirem
            if (!divergencias.isEmpty()) {
                System.out.println("\n================================================================================");
                System.out.println("                 DIVERGÊNCIAS DETECTADAS COM O CATÁLOGO DE PRODUTOS              ");
                System.out.println("================================================================================");
                for (String d : divergencias) {
                    System.out.println(d);
                }
                System.out.println("================================================================================");
                System.out.println("Aviso: O novo orçamento será criado mantendo os dados do orçamento original.");
                System.out.print("Deseja prosseguir com a duplicação mesmo com as divergências? (s/n): ");
            } else {
                System.out.print("\nDeseja confirmar a duplicação deste orçamento para um novo orçamento ABERTO? (s/n): ");
            }

            String confirmacao = entrada.nextLine().trim().toLowerCase();
            if (!confirmacao.equals("s") && !confirmacao.equals("sim")) {
                System.out.println("Duplicação cancelada.");
                return true;
            }

            // 4. Executar clonagem no banco
            Orcamento duplicado = orcamentoDAO.duplicar(origem.getIdOrcamento());
            if (duplicado == null) {
                System.out.println("Erro ao duplicar orçamento.");
                return true;
            }

            System.out.println("\n================================================================================");
            System.out.println("                        ORÇAMENTO DUPLICADO COM SUCESSO!                        ");
            System.out.println("================================================================================");
            System.out.println(String.format(" Origem: Orçamento #%d (Status: %s)", origem.getIdOrcamento(), origem.getStatusOrcamento()));
            System.out.println(String.format(" Novo:   Orçamento #%d (Nome: %s | Status: ABERTO)", duplicado.getIdOrcamento(), duplicado.getNomeOrcamento()));
            System.out.println(String.format(" Itens Copiados: %d | Valor Total: R$ %.2f", duplicado.getItensOrcamento().size(), duplicado.getValorTotal()));
            System.out.println(" Entrando na sessão do novo orçamento...");
            System.out.println("================================================================================");

            sessao.entrarOrcamento(duplicado.getIdOrcamento());

        } catch (SQLException e) {
            System.out.println("Erro ao duplicar orçamento: " + e.getMessage());
        }

        return true;
    }
}
