package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.Scanner;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.PerfilUsuario;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;

public class ComandoCadastrarUsuario extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoCadastrarUsuario(SessaoOrcamento sessao) {
        super(
                "/cadastrar_usuario",
                """
                Cadastra um novo operador no sistema.
                Solicita Nome, Login, Senha e Perfil (ADMIN, GERENTE, VENDEDOR, CAIXA).
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        if (!sessao.validarPermissao(Permissao.USUARIO_CADASTRAR)) {
            return true;
        }

        if (argumentos.length > 0) {
            System.out.println("Uso correto: /cadastrar_usuario");
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        UsuarioDAO usuarioDAO = sessao.getUsuarioDAO();

        System.out.print("Insira o nome completo do usuário: ");
        String nome = entrada.nextLine().trim();

        System.out.print("Insira o login de acesso: ");
        String login = entrada.nextLine().trim();

        String senha = sessao.lerSenha("Insira a senha inicial: ");

        PerfilUsuario perfil = null;
        while (perfil == null) {
            System.out.print("Insira o perfil do usuário (ADMIN, GERENTE, VENDEDOR, CAIXA): ");
            String perfilStr = entrada.nextLine().trim().toUpperCase();
            try {
                perfil = PerfilUsuario.valueOf(perfilStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Perfil inválido! Escolha entre: ADMIN, GERENTE, VENDEDOR, CAIXA.");
            }
        }

        try {
            Usuario usuario = usuarioDAO.cadastrar(nome, login, senha, perfil);
            if (usuario != null) {
                System.out.println("\n✅ Usuário cadastrado com sucesso!");
                System.out.println(usuario);
                System.out.println("Permissões concedidas inicialmente: " + usuario.getPermissoes().size() + " permissão(ões).");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Erro ao cadastrar usuário: " + e.getMessage());
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println("Erro: Já existe um usuário cadastrado com o login '" + login + "'.");
            } else {
                System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
            }
        }

        return true;
    }
}
