package projetopdv.ui.comandos;

import java.time.format.DateTimeParseException;
import java.util.Scanner;
import projetopdv.cliente.Cliente;
import projetopdv.cliente.ClienteDAO;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Permissao;

public class ComandoCadastrarCliente extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoCadastrarCliente(SessaoOrcamento sessao) {
        super(
                "/cadastrar_cliente",
                """
                Cadastra um novo cliente no sistema.
                Solicita Nome, Data de Nascimento e CPF.
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.CLIENTE_CADASTRAR)) {
            return true;
        }

        if (argumentos.length > 0) {
            System.out.println("Uso correto: /cadastrar_cliente");
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        ClienteDAO clienteDAO = sessao.getClienteDAO();

        System.out.print("Insira o nome do cliente: ");
        String nome = entrada.nextLine().trim();

        System.out.print("Insira a data de nascimento (DD/MM/AAAA): ");
        String dataNascimento = entrada.nextLine().trim();

        System.out.print("Insira o CPF: ");
        String cpf = entrada.nextLine().trim();

        try {
            Cliente cliente = clienteDAO.cadastrar(
                    nome,
                    dataNascimento,
                    cpf
            );

            if (cliente != null) {
                System.out.println("\n✅ Cliente cadastrado com sucesso!");
                System.out.println(cliente);
            }
        } catch (DateTimeParseException e) {
            System.out.println("Data de Nascimento Inválida! \nUse o formato DD/MM/AAAA.");
        } catch (IllegalArgumentException e) {
            System.out.println("Erro ao cadastrar cliente: " + e.getMessage());
        }

        return true;
    }
}
