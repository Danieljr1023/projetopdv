package projetopdv.ui;

import java.util.Scanner;
import projetopdv.cliente.ClienteDAO;
import projetopdv.dados.BancoDeDados;
import projetopdv.orcamento.OrcamentoDAO;
import projetopdv.produto.ProdutoDAO;
import projetopdv.ui.comandos.ComandoAbrirOrcamento;
import projetopdv.ui.comandos.ComandoCadastrarCliente;
import projetopdv.ui.comandos.ComandoCadastrarProduto;
import projetopdv.ui.comandos.ComandoCadastrarUsuario;
import projetopdv.ui.comandos.ComandoCliente;
import projetopdv.ui.comandos.ComandoConsultarCliente;
import projetopdv.ui.comandos.ComandoConsultarProdutos;
import projetopdv.ui.comandos.ComandoDuplicarOrcamento;
import projetopdv.ui.comandos.ComandoHelp;
import projetopdv.ui.comandos.ComandoListarOrcamentos;
import projetopdv.ui.comandos.ComandoLogin;
import projetopdv.ui.comandos.ComandoLogout;
import projetopdv.ui.comandos.ComandoNovoOrcamento;
import projetopdv.ui.comandos.ComandoProduto;
import projetopdv.ui.comandos.ComandoRecuperarOrcamento;
import projetopdv.ui.comandos.ComandoResetarBanco;
import projetopdv.ui.comandos.ComandoSair;
import projetopdv.ui.comandos.ComandoUsuario;
import projetopdv.ui.comandos.ComandosPainel;
import projetopdv.ui.comandos.orcamento.ComandoDescartarOrcamento;
import projetopdv.usuario.UsuarioDAO;

public class Painel {

    public static void main(String[] args) {

        try (Scanner entrada = new Scanner(System.in)) {
            ProdutoDAO produtoDAO = new ProdutoDAO();
            ClienteDAO clienteDAO = new ClienteDAO();
            OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
            UsuarioDAO usuarioDAO = new UsuarioDAO();

            BancoDeDados.inicializarTabelas();

            ComandosPainel comandos = new ComandosPainel();
            SessaoOrcamento sessao = new SessaoOrcamento(entrada, orcamentoDAO, produtoDAO, clienteDAO, usuarioDAO, comandos);

            // Comandos do Sistema e Autenticação
            comandos.registrar(new ComandoSair());
            comandos.registrar(new ComandoHelp(comandos));
            comandos.registrar(new ComandoLogin(sessao));
            comandos.registrar(new ComandoLogout(sessao));

            // Comandos de Usuário e Segurança
            comandos.registrar(new ComandoCadastrarUsuario(sessao));
            comandos.registrar(new ComandoUsuario(sessao));
            comandos.registrar(new ComandoResetarBanco(sessao));

            // Comandos de Produto
            comandos.registrar(new ComandoCadastrarProduto(sessao));
            comandos.registrar(new ComandoConsultarProdutos(sessao));
            comandos.registrar(new ComandoProduto(sessao));

            // Comandos de Cliente
            comandos.registrar(new ComandoCadastrarCliente(sessao));
            comandos.registrar(new ComandoConsultarCliente(sessao));
            comandos.registrar(new ComandoCliente(sessao));

            // Comandos de Orçamento (Menu Principal)
            comandos.registrar(new ComandoNovoOrcamento(sessao));
            comandos.registrar(new ComandoAbrirOrcamento(sessao));
            comandos.registrar(new ComandoRecuperarOrcamento(sessao));
            comandos.registrar(new ComandoListarOrcamentos(sessao));
            comandos.registrar(new ComandoDuplicarOrcamento(sessao));
            comandos.registrar(new ComandoDescartarOrcamento(sessao));

            boolean executando = true;

            System.out.println("\n=== SISTEMA PDV INICIADO ===");
            System.out.println("Digite /help para consultar os comandos.");

            while (executando) {
                System.out.print(sessao.obterPrompt());

                if (!entrada.hasNextLine()) {
                    break;
                }

                String linha = entrada.nextLine();
                try {
                    executando = sessao.processarLinha(linha);
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
