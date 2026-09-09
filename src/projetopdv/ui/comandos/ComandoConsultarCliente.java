package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.List;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoConsultarCliente extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoConsultarCliente(SessaoOrcamento sessao) {
        super(
                "/consultar_clientes",
                "Lista todos os clientes cadastrados."
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.CLIENTE_CONSULTAR)) {
            return true;
        }

        if (argumentos.length > 0) {
            System.out.println("Uso correto: /consultar_clientes");
            return true;
        }

        ClienteDAO clienteDAO = sessao.getClienteDAO();

        try {
            List<Cliente> clientes = clienteDAO.listarTodos();

            if (clientes.isEmpty()) {
                System.out.println("Nenhum cliente cadastrado.");
                return true;
            }

            System.out.println("\nClientes cadastrados:");
            for (Cliente cliente : clientes) {
                System.out.println(cliente);
            }

            return true;
        } catch (SQLException e) {
            System.out.println("Erro ao consultar clientes: " + e.getMessage());
            return true;
        }
    }
}
