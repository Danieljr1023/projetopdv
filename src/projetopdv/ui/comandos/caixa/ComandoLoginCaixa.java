package projetopdv.ui.comandos.caixa;

import projetopdv.ui.SessaoCaixa;
import projetopdv.ui.comandos.ComandoPainel;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;

public class ComandoLoginCaixa extends ComandoPainel {

    private final SessaoCaixa sessao;

    public ComandoLoginCaixa(SessaoCaixa sessao) {
        super("/login", "Autentica o operador de caixa no sistema.");
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        String login;
        String senha;

        if (argumentos.length >= 2) {
            login = argumentos[0].trim();
            senha = argumentos[1].trim();
        } else {
            System.out.print("Informe o login do operador: ");
            login = sessao.getEntrada().nextLine().trim();
            System.out.print("Informe a senha: ");
            senha = sessao.getEntrada().nextLine().trim();
        }

        if (login.isEmpty() || senha.isEmpty()) {
            System.out.println("Login e senha são obrigatórios.");
            return true;
        }

        Usuario usuario;
        try {
            usuario = sessao.getUsuarioDAO().autenticar(login, senha);
        } catch (java.sql.SQLException e) {
            System.out.println("[ERRO] Erro ao autenticar operador: " + e.getMessage());
            return true;
        }
        if (usuario == null) {
            System.out.println("[ERRO] Usuário ou senha inválidos.");
            return true;
        }

        if (!usuario.isAtivo()) {
            System.out.println("[ACESSO BLOQUEADO] O operador está desativado no sistema.");
            return true;
        }

        if (!usuario.temPermissao(Permissao.VENDA_FATURAR) && !usuario.temPermissao(Permissao.VENDA_TODAS)) {
            System.out.println("[ACESSO NEGADO] Este usuário não possui permissão de operador de caixa.");
            return true;
        }

        sessao.setUsuarioLogado(usuario);
        System.out.printf("✓ Operador '%s' conectado com sucesso! (Perfil: %s)%n", usuario.getNomeUsuario(), usuario.getPerfil().getNomeExibicao());

        if (!sessao.getCaixaDAO().isCaixaAberto()) {
            System.out.println("\n[AVISO] O caixa físico está fechado.");
            System.out.println("Para iniciar o turno, digite: /abrir_caixa <fundo_troco>");
        } else {
            System.out.println("O caixa físico já se encontra aberto e pronto para faturamento!");
        }

        return true;
    }
}
