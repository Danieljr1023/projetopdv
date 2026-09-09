package projetopdv.ui.comandos.orcamento;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;

public class ComandoSetCliente extends ComandoPainel {

    private final SessaoOrcamento sessao;
    private final OrcamentoDAO orcamentoDAO;
    private final ClienteDAO clienteDAO;

    public ComandoSetCliente(SessaoOrcamento sessao) {
        super(
                "/set_cliente",
                """
                Vincula um cliente ao orçamento ativo por ID, CPF, Nome ou modo interativo.

                Usos:
                /set_cliente                              (Modo interativo com busca/listagem e seleção rápida)
                /set_cliente <ID, CPF ou Nome do Cliente> (Vínculo direto)
                """
        );
        this.sessao = sessao;
        this.orcamentoDAO = sessao.getOrcamentoDAO();
        this.clienteDAO = sessao.getClienteDAO();
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

        Scanner entrada = sessao.getEntrada();
        String termo;

        if (argumentos.length < 1) {
            System.out.print("Informe o ID, CPF ou Nome do cliente (ou 'listar' / 'cancelar'): ");
            termo = entrada.nextLine().trim();

            if (termo.isEmpty() || termo.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação cancelada.");
                return true;
            }

            if (termo.equalsIgnoreCase("listar")) {
                try {
                    List<Cliente> todos = clienteDAO.listarTodos();
                    if (todos.isEmpty()) {
                        System.out.println("Nenhum cliente cadastrado.");
                        return true;
                    }
                    System.out.println("\n=== CLIENTES CADASTRADOS ===");
                    for (int i = 0; i < todos.size(); i++) {
                        Cliente c = todos.get(i);
                        System.out.printf("[%d] ID: %-4d | %-28s | CPF: %s%n",
                                (i + 1), c.getIdCliente(), c.getNomeCliente(),
                                c.getCpf().isEmpty() ? "Não informado" : c.getCpf());
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
                            return vincularCliente(todos.get(num - 1));
                        }
                        Cliente porId = clienteDAO.buscarPorId(num);
                        if (porId != null) {
                            return vincularCliente(porId);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    System.out.println("Opção inválida.");
                    return true;
                } catch (SQLException e) {
                    System.out.println("Erro ao listar clientes: " + e.getMessage());
                    return true;
                }
            }
        } else {
            termo = String.join(" ", argumentos).trim();
        }

        Cliente cliente = null;

        // 1. Tenta por ID
        try {
            int id = Integer.parseInt(termo);
            cliente = clienteDAO.buscarPorId(id);
        } catch (NumberFormatException ignored) {
        }

        // 2. Tenta por CPF
        if (cliente == null) {
            cliente = clienteDAO.buscarPorCpf(termo);
        }

        // 3. Tenta por Nome
        if (cliente == null) {
            List<Cliente> encontrados = clienteDAO.pesquisarPorTermo(termo);
            if (encontrados.size() == 1) {
                cliente = encontrados.get(0);
            } else if (encontrados.size() > 1) {
                System.out.println("\nForam encontrados múltiplos clientes para '" + termo + "':");
                for (int i = 0; i < encontrados.size(); i++) {
                    Cliente c = encontrados.get(i);
                    System.out.printf("[%d] ID: %-4d | Nome: %-25s | CPF: %s%n",
                            (i + 1), c.getIdCliente(), c.getNomeCliente(),
                            c.getCpf().isEmpty() ? "Não informado" : c.getCpf());
                }
                System.out.print("Escolha o número da opção desejada (ou 'cancelar'): ");
                String sel = entrada.nextLine().trim();
                if (sel.isEmpty() || sel.equalsIgnoreCase("cancelar")) {
                    System.out.println("Operação cancelada.");
                    return true;
                }
                try {
                    int op = Integer.parseInt(sel);
                    if (op >= 1 && op <= encontrados.size()) {
                        cliente = encontrados.get(op - 1);
                    }
                } catch (NumberFormatException ignored) {
                }
                if (cliente == null) {
                    System.out.println("Opção inválida.");
                    return true;
                }
            }
        }

        if (cliente == null) {
            System.out.println("Cliente '" + termo + "' não encontrado.");
            return true;
        }

        return vincularCliente(cliente);
    }

    private boolean vincularCliente(Cliente cliente) {

        try {
            int idOrcamento = sessao.getIdOrcamentoAtivo();
            boolean vinculado = orcamentoDAO.vincularCliente(idOrcamento, cliente.getIdCliente());
            if (vinculado) {
                System.out.println("\n[CLIENTE VINCULADO AO ORÇAMENTO]");
                System.out.println(String.format("Cliente: ID: %d | Nome: %s | CPF: %s",
                        cliente.getIdCliente(), cliente.getNomeCliente(), cliente.getCpf().isEmpty() ? "Não informado" : cliente.getCpf()));
            } else {
                System.out.println("Não foi possível vincular o cliente ao orçamento.");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao vincular cliente no banco: " + e.getMessage());
        }

        return true;
    }
}
