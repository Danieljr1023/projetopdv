package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.List;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoListarOrcamentos extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoListarOrcamentos(SessaoOrcamento sessao) {
        super(
                "/orcamentos",
                """
                Lista ou exibe detalhes de orçamentos cadastrados (somente leitura).

                Usos:
                /orcamentos                  (Lista todos os orçamentos)
                /orcamentos <Status>         (Filtra por status: ABERTO, CONFIRMADO, CANCELADO, FATURANDO, FINALIZADO)
                /orcamentos <ID ou Nome>     (Exibe o cupom/resumo detalhado do orçamento sem abrir para edição)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_CONSULTAR)) {
            return true;
        }

        try {
            List<Orcamento> orcamentos;

            if (argumentos.length == 0) {
                orcamentos = orcamentoDAO.listarTodos();
            } else {
                String arg = argumentos[0].trim();
                try {
                    StatusOrcamento status = StatusOrcamento.valueOf(arg.toUpperCase());
                    orcamentos = orcamentoDAO.listarPorStatus(status);
                } catch (IllegalArgumentException e) {
                    // Tenta buscar por ID para exibição detalhada (somente leitura)
                    try {
                        int id = Integer.parseInt(arg);
                        Orcamento o = orcamentoDAO.buscarPorId(id);
                        if (o != null) {
                            sessao.imprimirResumoCompleto(o);
                            return true;
                        }
                    } catch (NumberFormatException ignored) {
                    }

                    // Tenta buscar por termo/nome
                    String termo = String.join(" ", argumentos).trim();
                    List<Orcamento> porNome = orcamentoDAO.buscarPorNome(termo);
                    if (porNome.size() == 1) {
                        sessao.imprimirResumoCompleto(porNome.get(0));
                        return true;
                    } else if (!porNome.isEmpty()) {
                        orcamentos = porNome;
                    } else {
                        System.out.println("Nenhum orçamento encontrado para '" + termo + "' (ou status inválido: ABERTO, CONFIRMADO, CANCELADO, FATURANDO, FINALIZADO).");
                        return true;
                    }
                }
            }

            if (orcamentos.isEmpty()) {
                System.out.println("\nNenhum orçamento encontrado.");
                return true;
            }

            System.out.println("\n====================================================================================================");
            System.out.println("                                      LISTA DE ORÇAMENTOS                                           ");
            System.out.println("====================================================================================================");
            System.out.println(String.format(" %-4s | %-24s | %-12s | %-20s | %-6s | %-12s",
                    "ID", "Nome", "Status", "Cliente", "Itens", "Valor Total"));
            System.out.println("----------------------------------------------------------------------------------------------------");

            for (Orcamento o : orcamentos) {
                String nome;
                try {
                    nome = o.getNomeOrcamento();
                } catch (Exception e) {
                    nome = "Sem Nome";
                }
                if (nome.length() > 24) {
                    nome = nome.substring(0, 21) + "...";
                }

                String clienteStr = o.temCliente() ? o.getCliente().getNomeCliente() : "Não informado";
                if (clienteStr.length() > 20) {
                    clienteStr = clienteStr.substring(0, 17) + "...";
                }

                System.out.println(String.format(" %-4d | %-24s | %-12s | %-20s | %-6d | R$ %9.2f",
                        o.getIdOrcamento(),
                        nome,
                        o.getStatusOrcamento(),
                        clienteStr,
                        o.getItensOrcamento().size(),
                        o.getValorTotal()));
            }

            System.out.println("====================================================================================================");

        } catch (SQLException e) {
            System.out.println("Erro ao listar orçamentos: " + e.getMessage());
        }

        return true;
    }
}
