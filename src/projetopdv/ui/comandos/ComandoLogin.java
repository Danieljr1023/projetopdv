package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.Scanner;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;

public class ComandoLogin extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoLogin(SessaoOrcamento sessao) {
        super(
                "/login",
                """
                Realiza a autenticação de um operador no sistema.

                Usos:
                /login                         (Modo interativo: solicita usuário e senha com máscara)
                /login <Usuário> <Senha>       (Login direto via linha de comando)
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (sessao.isAutenticado()) {
            System.out.println("Já existe um usuário autenticado: " + sessao.getUsuarioLogado().getNomeUsuario()
                    + " [" + sessao.getUsuarioLogado().getPerfil().name() + "]. Digite /logout para encerrar a sessão.");
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        UsuarioDAO usuarioDAO = sessao.getUsuarioDAO();

        String login;
        String senha;

        if (argumentos.length >= 2) {
            login = argumentos[0].trim();
            senha = argumentos[1].trim();
        } else if (argumentos.length == 1) {
            login = argumentos[0].trim();
            senha = sessao.lerSenha("Digite a senha: ");
        } else {
            System.out.print("Digite o usuário: ");
            login = entrada.nextLine().trim();
            senha = sessao.lerSenha("Digite a senha: ");
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(login, senha);

            if (usuario != null) {
                sessao.setUsuarioLogado(usuario);
                System.out.println("\n✅ Login realizado com sucesso! Bem-vindo, " + usuario.getNomeUsuario()
                        + " [" + usuario.getPerfil().name() + "].");
            } else {
                System.out.println("\n❌ Erro: Usuário ou senha incorretos (ou usuário desativado).");
            }

        } catch (SQLException e) {
            System.out.println("Erro ao autenticar no banco de dados: " + e.getMessage());
        }

        return true;
    }
}
