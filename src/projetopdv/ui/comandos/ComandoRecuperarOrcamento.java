package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.List;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.Orcamento.StatusOrcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoRecuperarOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;

    public ComandoRecuperarOrcamento(SessaoOrcamento sessao) {
        super(
                "/recuperar",
                """
                Recupera e reabre um orçamento cancelado ou lista o histórico para recuperação.

                Usos:
                /recuperar               (Lista orçamentos CANCELADOS com seleção rápida)
                /recuperar <ID ou Nome>  (Reabre o orçamento cancelado diretamente)
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
            if (argumentos.length == 0) {
                List<Orcamento> cancelados = orcamentoDAO.listarPorStatus(StatusOrcamento.CANCELADO);
                if (cancelados.isEmpty()) {
                    System.out.println("\nNenhum orçamento com status CANCELADO encontrado para recuperação.");
                    return true;
                }

                System.out.println("\n================================================================================");
                System.out.println("                 HISTÓRICO DE ORÇAMENTOS CANCELADOS PARA RECUPERAÇÃO             ");
                System.out.println("================================================================================");
                for (int i = 0; i < cancelados.size(); i++) {
                    Orcamento o = cancelados.get(i);
                    String clienteStr = o.temCliente() ? o.getCliente().getNomeCliente() : "Não informado";
                    String nome;
                    try {
                        nome = o.getNomeOrcamento();
                    } catch (Exception e) {
                        nome = "Sem Nome";
                    }
                    System.out.println(String.format("[%d] ID: %-4d | Nome: %-25s | Cliente: %-20s | Total: R$ %9.2f",
                            (i + 1), o.getIdOrcamento(), nome, clienteStr, o.getValorTotal()));
                }
                System.out.println("================================================================================");
                System.out.println("Digite o número da opção para recuperar e abrir (ou digite outro comando):");

                sessao.definirOpcoesRecuperarPendentes(cancelados);
                return true;
            }

            String termo = String.join(" ", argumentos).trim();
            if (termo.toLowerCase().startsWith("id ")) {
                termo = termo.substring(3).trim();
            } else if (termo.toLowerCase().startsWith("nome ")) {
                termo = termo.substring(5).trim();
            }

            // Tenta por ID (estritamente CANCELADO)
            try {
                int id = Integer.parseInt(termo);
                Orcamento o = orcamentoDAO.buscarPorId(id);
                if (o != null && o.getStatusOrcamento() == StatusOrcamento.CANCELADO) {
                    sessao.recuperarOrcamento(id);
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }

            // Tenta por nome (estritamente CANCELADO)
            List<Orcamento> porNome = orcamentoDAO.buscarPorNome(termo).stream()
                    .filter(o -> o.getStatusOrcamento() == StatusOrcamento.CANCELADO)
                    .toList();

            if (porNome.size() == 1) {
                sessao.recuperarOrcamento(porNome.get(0).getIdOrcamento());
            } else if (porNome.size() > 1) {
                System.out.println("\nForam encontrados " + porNome.size() + " orçamentos para '" + termo + "':");
                for (int i = 0; i < porNome.size(); i++) {
                    Orcamento o = porNome.get(i);
                    String nome;
                    try {
                        nome = o.getNomeOrcamento();
                    } catch (Exception e) {
                        nome = "Sem Nome";
                    }
                    System.out.println(String.format("[%d] ID: %-4d | Nome: %-25s | Total: R$ %9.2f",
                            (i + 1), o.getIdOrcamento(), nome, o.getValorTotal()));
                }
                System.out.println("Digite o número da opção desejada para recuperar:");
                sessao.definirOpcoesRecuperarPendentes(porNome);
            } else {
                System.out.println("Orçamento não encontrado.");
            }

        } catch (SQLException e) {
            System.out.println("Erro ao acessar banco de dados: " + e.getMessage());
        }

        return true;
    }
}
