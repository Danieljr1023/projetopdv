package projetopdv.ui.comandos;

import java.util.List;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.orcamento.Orcamento;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoNovoOrcamento extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;
    private final ClienteDAO clienteDAO;

    public ComandoNovoOrcamento(SessaoOrcamento sessao) {
        super(
                "/novo_orcamento",
                """
                Cria um novo orçamento aberto e entra automaticamente na sua sessão.

                Usos:
                /novo_orcamento
                /novo_orcamento <Nome do Orçamento>
                /novo_orcamento <Nome do Orçamento> <ID, CPF ou Nome do Cliente>
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
        this.clienteDAO = sessao.getClienteDAO();
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.ORCAMENTO_CRIAR)) {
            return true;
        }

        String nomeOrcamento = "";
        Integer idCliente = null;
        Cliente cliente = null;

        if (argumentos.length >= 1) {
            nomeOrcamento = argumentos[0].trim();
            if (nomeOrcamento.contains("%")) {
                System.out.println("O nome do orçamento não pode conter o caractere '%'.");
                return true;
            }
        }

        if (argumentos.length >= 2) {
            String argCliente = argumentos[1].trim();
            // Tenta por ID
            try {
                int id = Integer.parseInt(argCliente);
                cliente = clienteDAO.buscarPorId(id);
            } catch (NumberFormatException e) {
                // Tenta por CPF
                cliente = clienteDAO.buscarPorCpf(argCliente);
                if (cliente == null) {
                    List<Cliente> encontrados = clienteDAO.pesquisarPorTermo(argCliente);
                    if (encontrados.size() == 1) {
                        cliente = encontrados.get(0);
                    } else if (encontrados.size() > 1) {
                        System.out.println("Mais de um cliente encontrado para '" + argCliente + "'. Vincule o cliente após criar o orçamento com /set_cliente.");
                    }
                }
            }

            if (cliente != null) {
                idCliente = cliente.getIdCliente();
            }
        }

        try {
            Orcamento novo = orcamentoDAO.cadastrar(nomeOrcamento, idCliente);
            if (novo == null) {
                System.out.println("Não foi possível criar o orçamento.");
                return true;
            }

            System.out.println("\n================================================================================");
            System.out.println(String.format("                     NOVO ORÇAMENTO CRIADO: #%04d                     ", novo.getIdOrcamento()));
            System.out.println("================================================================================");
            String nomeExibicao = nomeOrcamento.isEmpty() ? "Sem Nome (definir antes de confirmar)" : nomeOrcamento;
            System.out.println(" Nome: " + nomeExibicao);
            System.out.println(" Status: " + novo.getStatusOrcamento());
            System.out.println(" Cliente: " + (cliente != null ? cliente.getNomeCliente() + " (ID: " + cliente.getIdCliente() + ")" : "Não informado"));
            System.out.println("================================================================================");

            sessao.entrarOrcamento(novo.getIdOrcamento());

        } catch (Exception e) {
            System.out.println("Erro ao criar orçamento: " + e.getMessage());
        }

        return true;
    }
}
