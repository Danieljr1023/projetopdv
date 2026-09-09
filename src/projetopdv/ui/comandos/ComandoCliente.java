package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoCliente extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoCliente(SessaoOrcamento sessao) {
        super(
                "/cliente",
                """
                Informa ou altera os dados de um cliente através do ID ou painel interativo.

                Usos:
                /cliente                                            (Modo interativo com busca/listagem e menu de opções)
                /cliente <ID do Cliente>                            (Abre o painel interativo de opções para o cliente)
                /cliente <ID do Cliente> <Subcomando> [Argumentos]  (Edição direta via comando)

                Argumentos de Consulta:
                    nome            Nome do cliente
                    data_nasc       Data de nascimento do cliente
                    cpf             CPF do cliente

                Argumentos de Edição:
                    set_nome        Define um novo nome para o cliente
                    set_data_nasc   Define uma nova data de nascimento para o cliente
                    set_cpf         Define um novo CPF para o cliente

                Argumentos de Exclusão:
                    deletar         Deleta o cliente
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        // ==========================================
        // CASO 1: SEM ARGUMENTOS (/cliente)
        // ==========================================
        if (argumentos.length == 0) {
            if (!sessao.validarPermissao(Permissao.CLIENTE_CONSULTAR)) {
                return true;
            }

            Scanner entrada = sessao.getEntrada();
            ClienteDAO clienteDAO = sessao.getClienteDAO();

            System.out.print("Informe o ID, CPF ou Nome do cliente (ou 'listar' / 'cancelar'): ");
            String busca = entrada.nextLine().trim();

            if (busca.isEmpty() || busca.equalsIgnoreCase("cancelar")) {
                System.out.println("Operação cancelada.");
                return true;
            }

            if (busca.equalsIgnoreCase("listar")) {
                try {
                    List<Cliente> todos = clienteDAO.listarTodos();
                    if (todos.isEmpty()) {
                        System.out.println("Nenhum cliente cadastrado.");
                        return true;
                    }
                    System.out.println("\n=== CLIENTES CADASTRADOS ===");
                    for (int i = 0; i < todos.size(); i++) {
                        Cliente c = todos.get(i);
                        System.out.printf("[%d] ID: %-4d | %-28s | CPF: %-14s | Nasc: %s%n",
                                (i + 1), c.getIdCliente(), c.getNomeCliente(),
                                c.getCpf().isEmpty() ? "Não informado" : c.getCpf(),
                                c.getDataNascimento() != null ? c.getDataNascimento().format(Cliente.FORMATO_DATA) : "Não informada");
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
                            return gerenciarClienteInterativo(todos.get(num - 1));
                        }
                        Cliente porId = clienteDAO.buscarPorId(num);
                        if (porId != null) {
                            return gerenciarClienteInterativo(porId);
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

            // Tenta por ID
            try {
                int id = Integer.parseInt(busca);
                Cliente c = clienteDAO.buscarPorId(id);
                if (c != null) {
                    return gerenciarClienteInterativo(c);
                }
            } catch (NumberFormatException ignored) {
            }

            // Tenta por CPF
            Cliente porCpf = clienteDAO.buscarPorCpf(busca);
            if (porCpf != null) {
                return gerenciarClienteInterativo(porCpf);
            }

            // Tenta por Nome / termo
            List<Cliente> encontrados = clienteDAO.pesquisarPorTermo(busca);
            if (encontrados.isEmpty()) {
                System.out.println("Nenhum cliente encontrado para '" + busca + "'.");
                return true;
            }

            if (encontrados.size() == 1) {
                return gerenciarClienteInterativo(encontrados.get(0));
            }

            System.out.println("\nForam encontrados " + encontrados.size() + " clientes para '" + busca + "':");
            for (int i = 0; i < encontrados.size(); i++) {
                Cliente c = encontrados.get(i);
                System.out.printf("[%d] ID: %-4d | %-28s | CPF: %s%n",
                        (i + 1), c.getIdCliente(), c.getNomeCliente(),
                        c.getCpf().isEmpty() ? "Não informado" : c.getCpf());
            }
            System.out.print("Escolha o número do cliente desejado (ou 'cancelar'): ");
            String esc = entrada.nextLine().trim();
            if (esc.equalsIgnoreCase("cancelar") || esc.isEmpty()) {
                System.out.println("Operação cancelada.");
                return true;
            }
            try {
                int op = Integer.parseInt(esc);
                if (op >= 1 && op <= encontrados.size()) {
                    return gerenciarClienteInterativo(encontrados.get(op - 1));
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.println("Opção inválida.");
            return true;
        }

        // ==========================================
        // CASO 2: COM ARGUMENTOS
        // ==========================================
        ClienteDAO clienteDAO = sessao.getClienteDAO();

        int idCliente;
        try {
            idCliente = Integer.parseInt(argumentos[0]);
        } catch (NumberFormatException e) {
            System.out.println("ID do cliente inválido.");
            return true;
        }

        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            System.out.println("Cliente não encontrado.");
            return true;
        }

        // Se informou apenas o ID (/cliente <ID>), abre o painel interativo do cliente
        if (argumentos.length == 1) {
            return gerenciarClienteInterativo(cliente);
        }

        String arg = argumentos[1].toLowerCase();

        switch (arg) {
            // =========================
            // CONSULTAS
            // =========================
            case "nome" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_CONSULTAR)) return true;
                System.out.println(cliente.getNomeCliente());
            }

            case "data_nasc" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_CONSULTAR)) return true;
                System.out.println(
                        cliente.getDataNascimento() != null
                                ? cliente.getDataNascimento().format(Cliente.FORMATO_DATA)
                                : "Não informada"
                );
            }

            case "cpf" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_CONSULTAR)) return true;
                System.out.println(cliente.getCpf().isEmpty() ? "Não informado" : cliente.getCpf());
            }

            // =========================
            // ALTERAR NOME
            // =========================
            case "set_nome" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EDITAR)) {
                    return true;
                }

                if (argumentos.length < 3) {
                    System.out.println("Uso: /cliente <ID> set_nome <Novo Nome>");
                    return true;
                }

                String novoNome = String.join(" ", java.util.Arrays.copyOfRange(argumentos, 2, argumentos.length)).trim();

                try {
                    cliente.setNomeCliente(novoNome);
                    if (clienteDAO.atualizar(cliente)) {
                        System.out.println("Nome alterado com sucesso.");
                    } else {
                        System.out.println("Não foi possível alterar o nome.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar nome: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar cliente: " + e.getMessage());
                }
            }

            // =========================
            // ALTERAR DATA DE NASCIMENTO
            // =========================
            case "set_data_nasc" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EDITAR)) {
                    return true;
                }

                if (argumentos.length != 3) {
                    System.out.println("Uso: /cliente <ID> set_data_nasc <Nova Data de Nascimento>");
                    return true;
                }

                try {
                    String novaDataNascimentoStr = argumentos[2];
                    LocalDate novaDataNascimento = LocalDate.parse(
                            novaDataNascimentoStr,
                            Cliente.FORMATO_DATA
                    );

                    cliente.setDataNascimento(novaDataNascimento);

                    if (clienteDAO.atualizar(cliente)) {
                        System.out.println("Data de nascimento alterada com sucesso.");
                    } else {
                        System.out.println("Não foi possível alterar a data de nascimento.");
                    }

                } catch (DateTimeParseException e) {
                    System.out.println("Data de nascimento inválida. Use o formato DD/MM/AAAA.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar data de nascimento: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar cliente: " + e.getMessage());
                }
            }

            // =========================
            // ALTERAR CPF
            // =========================
            case "set_cpf" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EDITAR)) {
                    return true;
                }

                if (argumentos.length != 3) {
                    System.out.println("Uso: /cliente <ID> set_cpf <Novo CPF>");
                    return true;
                }

                String novoCPF = argumentos[2];

                try {
                    cliente.setCpf(novoCPF);
                    if (clienteDAO.atualizar(cliente)) {
                        System.out.println("CPF alterado com sucesso.");
                    } else {
                        System.out.println("Não foi possível alterar o CPF.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar CPF: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar cliente: " + e.getMessage());
                }
            }

            // =========================
            // DELETAR
            // =========================
            case "deletar" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EXCLUIR)) {
                    return true;
                }

                if (argumentos.length != 2) {
                    System.out.println("Uso: /cliente <ID> deletar");
                    return true;
                }

                try {
                    if (clienteDAO.deletar(idCliente)) {
                        System.out.println("Cliente deletado com sucesso.");
                    } else {
                        System.out.println("Cliente não encontrado.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
                }
            }

            // =========================
            // INVÁLIDO
            // =========================
            default -> System.out.println("Argumento inválido. Digite /help /cliente para ver as opções.");
        }

        return true;
    }

    private boolean gerenciarClienteInterativo(Cliente cliente) {
        if (!sessao.validarPermissao(Permissao.CLIENTE_CONSULTAR)) {
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        ClienteDAO clienteDAO = sessao.getClienteDAO();

        System.out.println("\n" + cliente);
        System.out.println("==================================================");
        System.out.println("           PAINEL DE GESTÃO DO CLIENTE            ");
        System.out.println("==================================================");
        System.out.printf("Cliente: #%d - %s%n", cliente.getIdCliente(), cliente.getNomeCliente());
        System.out.printf("CPF: %s | Nasc: %s%n",
                cliente.getCpf().isEmpty() ? "Não informado" : cliente.getCpf(),
                cliente.getDataNascimento() != null ? cliente.getDataNascimento().format(Cliente.FORMATO_DATA) : "Não informada");
        System.out.println("--------------------------------------------------");
        System.out.println("[1] Consultar dados detalhados");
        System.out.println("[2] Alterar nome");
        System.out.println("[3] Alterar data de nascimento");
        System.out.println("[4] Alterar CPF");
        System.out.println("[5] Deletar cliente");
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
                System.out.println("\n" + cliente);
            }
            case "2" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EDITAR)) return true;
                System.out.printf("Nome atual: %s%n", cliente.getNomeCliente());
                System.out.print("Informe o novo nome (ou 'cancelar'): ");
                String novoNome = entrada.nextLine().trim();
                if (novoNome.isEmpty() || novoNome.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    cliente.setNomeCliente(novoNome);
                    if (clienteDAO.atualizar(cliente)) {
                        System.out.println("✅ Nome alterado com sucesso para: " + novoNome);
                    } else {
                        System.out.println("Não foi possível alterar o nome.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar nome: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar cliente: " + e.getMessage());
                }
            }
            case "3" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EDITAR)) return true;
                System.out.printf("Data atual: %s%n", cliente.getDataNascimento() != null ? cliente.getDataNascimento().format(Cliente.FORMATO_DATA) : "Não informada");
                System.out.print("Informe a nova data de nascimento (DD/MM/AAAA) (ou 'cancelar'): ");
                String dataStr = entrada.nextLine().trim();
                if (dataStr.isEmpty() || dataStr.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    LocalDate novaData = LocalDate.parse(dataStr, Cliente.FORMATO_DATA);
                    cliente.setDataNascimento(novaData);
                    if (clienteDAO.atualizar(cliente)) {
                        System.out.println("✅ Data de nascimento alterada com sucesso!");
                    } else {
                        System.out.println("Não foi possível alterar a data de nascimento.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("Data inválida. Use o formato DD/MM/AAAA.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar data: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar cliente: " + e.getMessage());
                }
            }
            case "4" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EDITAR)) return true;
                System.out.printf("CPF atual: %s%n", cliente.getCpf().isEmpty() ? "Não informado" : cliente.getCpf());
                System.out.print("Informe o novo CPF (apenas números ou formatado) (ou 'cancelar'): ");
                String novoCpf = entrada.nextLine().trim();
                if (novoCpf.isEmpty() || novoCpf.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    cliente.setCpf(novoCpf);
                    if (clienteDAO.atualizar(cliente)) {
                        System.out.println("✅ CPF alterado com sucesso!");
                    } else {
                        System.out.println("Não foi possível alterar o CPF.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar CPF: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar cliente: " + e.getMessage());
                }
            }
            case "5" -> {
                if (!sessao.validarPermissao(Permissao.CLIENTE_EXCLUIR)) return true;
                System.out.printf("⚠️ Tem certeza que deseja deletar o cliente #%d - %s permanentemente? (s/n): ",
                        cliente.getIdCliente(), cliente.getNomeCliente());
                String conf = entrada.nextLine().trim().toLowerCase();
                if (!conf.equals("s") && !conf.equals("sim")) {
                    System.out.println("Exclusão cancelada.");
                    return true;
                }
                try {
                    if (clienteDAO.deletar(cliente.getIdCliente())) {
                        System.out.println("✅ Cliente deletado com sucesso.");
                    } else {
                        System.out.println("Não foi possível deletar o cliente.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao excluir cliente no banco: " + e.getMessage());
                }
            }
            default -> System.out.println("Opção inválida.");
        }

        return true;
    }
}
