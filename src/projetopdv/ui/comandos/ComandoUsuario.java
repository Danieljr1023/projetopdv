package projetopdv.ui.comandos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import projetopdv.ui.SessaoOrcamento;
import projetopdv.usuario.PerfilUsuario;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;
import projetopdv.usuario.UsuarioDAO;

public class ComandoUsuario extends ComandoPainel {

    private final SessaoOrcamento sessao;

    public ComandoUsuario(SessaoOrcamento sessao) {
        super(
                "/usuario",
                """
                Informa ou altera os dados e permissões de um operador através do ID ou painel interativo.

                Usos:
                /usuario                                         (Lista operadores e permite escolher um para gestão interativa)
                /usuario <ID do Usuário>                         (Abre o painel interativo de opções para o operador)
                /usuario <ID do Usuário> <Subcomando> [Argumentos] (Edição direta via comando)

                Subcomandos de Consulta:
                    permissoes                               Lista todas as permissões ativas, extras e revogadas

                Subcomandos de Edição:
                    set_nome <Novo Nome>                     Define um novo nome para o usuário
                    set_perfil <ADMIN|GERENTE|VENDEDOR|CAIXA> Define um novo perfil base para o usuário
                    trocar_senha <Nova Senha>                Define uma nova senha de acesso para o usuário

                Subcomandos de Gestão de Permissões:
                    conceder <NOME_PERMISSAO>                Concede uma permissão especial individual
                    revogar <NOME_PERMISSAO>                 Revoga uma permissão específica
                    reset_permissoes                         Restaura as permissões para o padrão do perfil

                Subcomandos de Status e Exclusão:
                    desativar                                Desativa o operador (bloqueia login)
                    ativar                                   Reativa o operador
                    deletar                                  Remove o operador permanentemente
                """
        );
        this.sessao = sessao;
    }

    @Override
    public boolean executar(String[] argumentos) {
        UsuarioDAO usuarioDAO = sessao.getUsuarioDAO();

        // 1. Listagem Geral (/usuario sem argumentos)
        if (argumentos.length == 0) {
            if (!sessao.validarPermissao(Permissao.USUARIO_CONSULTAR)) {
                return true;
            }

            try {
                List<Usuario> usuarios = usuarioDAO.listarTodos();
                if (usuarios.isEmpty()) {
                    System.out.println("Nenhum usuário cadastrado.");
                    return true;
                }

                System.out.println("\n=== USUÁRIOS CADASTRADOS ===");
                for (Usuario u : usuarios) {
                    System.out.println(u);
                }

                Scanner entrada = sessao.getEntrada();
                System.out.print("\nInforme o ID do operador para gerenciar (ou 0 / 'cancelar'): ");
                String inputId = entrada.nextLine().trim();
                if (inputId.isEmpty() || inputId.equals("0") || inputId.equalsIgnoreCase("cancelar")) {
                    return true;
                }

                int idEscolhido;
                try {
                    idEscolhido = Integer.parseInt(inputId);
                } catch (NumberFormatException e) {
                    System.out.println("ID inválido.");
                    return true;
                }

                Usuario uEscolhido = usuarioDAO.buscarPorId(idEscolhido);
                if (uEscolhido == null) {
                    System.out.println("Usuário com ID " + idEscolhido + " não encontrado.");
                    return true;
                }

                return gerenciarUsuarioInterativo(uEscolhido);
            } catch (SQLException e) {
                System.out.println("Erro ao listar usuários: " + e.getMessage());
            }
            return true;
        }

        // 2. Obter ID do Usuário
        int idUsuario;
        try {
            idUsuario = Integer.parseInt(argumentos[0]);
        } catch (NumberFormatException e) {
            System.out.println("ID de usuário inválido: " + argumentos[0]);
            return true;
        }

        Usuario usuario;
        try {
            usuario = usuarioDAO.buscarPorId(idUsuario);
            if (usuario == null) {
                System.out.println("Usuário com ID " + idUsuario + " não encontrado.");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Erro ao buscar usuário: " + e.getMessage());
            return true;
        }

        // 3. Exibição Detalhada e Painel Interativo (/usuario <ID>)
        if (argumentos.length == 1) {
            return gerenciarUsuarioInterativo(usuario);
        }

        String subcomando = argumentos[1].toLowerCase();

        switch (subcomando) {
            // =========================
            // LISTAR PERMISSÕES
            // =========================
            case "permissoes" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_CONSULTAR)) {
                    return true;
                }
                System.out.println("\n=== PERMISSÕES DE " + usuario.getNomeUsuario().toUpperCase() + " (" + usuario.getPerfil().name() + ") ===");
                for (Permissao p : Permissao.values()) {
                    boolean ativa = usuario.temPermissao(p);
                    String prefixo = p.isMae() ? "📁 [MÓDULO] " : "  └── 🔹 ";
                    System.out.println(String.format("%s%-32s : %s (%s)", prefixo, p.name(),
                            ativa ? "✅ ATIVA" : "❌ INATIVA", p.getDescricao()));
                }
            }

            // =========================
            // SET NOME
            // =========================
            case "set_nome" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_EDITAR)) {
                    return true;
                }
                if (argumentos.length < 3) {
                    System.out.println("Uso: /usuario <ID> set_nome <Novo Nome>");
                    return true;
                }
                String novoNome = String.join(" ", java.util.Arrays.copyOfRange(argumentos, 2, argumentos.length)).trim();
                try {
                    usuario.setNomeUsuario(novoNome);
                    if (usuarioDAO.atualizar(usuario)) {
                        System.out.println("Nome atualizado com sucesso!");
                    } else {
                        System.out.println("Não foi possível atualizar o nome.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar nome: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar usuário no banco: " + e.getMessage());
                }
            }

            // =========================
            // SET PERFIL
            // =========================
            case "set_perfil" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_EDITAR)) {
                    return true;
                }
                if (argumentos.length != 3) {
                    System.out.println("Uso: /usuario <ID> set_perfil <ADMIN|GERENTE|VENDEDOR|CAIXA>");
                    return true;
                }
                try {
                    PerfilUsuario novoPerfil = PerfilUsuario.valueOf(argumentos[2].toUpperCase());
                    usuario.setPerfil(novoPerfil);
                    usuario.redefinirParaPadraoDoPerfil();
                    if (usuarioDAO.atualizar(usuario)) {
                        System.out.println("Perfil alterado para " + novoPerfil.name() + " e permissões redefinidas para o padrão do novo perfil com sucesso!");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Perfil inválido! Escolha entre: ADMIN, GERENTE, VENDEDOR, CAIXA.");
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar perfil no banco: " + e.getMessage());
                }
            }

            // =========================
            // TROCAR SENHA
            // =========================
            case "trocar_senha" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_SENHA)) {
                    return true;
                }
                String novaSenha;
                switch (argumentos.length) {
                    case 2 -> novaSenha = sessao.lerSenha("Digite a nova senha para " + usuario.getNomeUsuario() + ": ");
                    case 3 -> novaSenha = argumentos[2];
                    default -> {
                        System.out.println("Uso: /usuario <ID> trocar_senha");
                        return true;
                    }
                }
                try {
                    if (usuarioDAO.alterarSenha(usuario.getIdUsuario(), novaSenha)) {
                        System.out.println("Senha alterada com sucesso!");
                    } else {
                        System.out.println("Não foi possível alterar a senha.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar senha no banco: " + e.getMessage());
                }
            }

            // =========================
            // CONCEDER PERMISSÃO
            // =========================
            case "conceder" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_PERMISSOES)) {
                    return true;
                }
                if (argumentos.length != 3) {
                    System.out.println("Uso: /usuario <ID> conceder <NOME_PERMISSAO>");
                    return true;
                }
                try {
                    Permissao perm = Permissao.valueOf(argumentos[2].toUpperCase());
                    usuario.concederPermissao(perm);
                    if (usuarioDAO.atualizarPermissoes(usuario.getIdUsuario(), usuario.getPermissoes())) {
                        System.out.println("Permissão " + perm.name() + " concedida com sucesso a " + usuario.getNomeUsuario() + "!");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Permissão inválida! Digite /usuario " + usuario.getIdUsuario() + " permissoes para ver a lista válida.");
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar permissões no banco: " + e.getMessage());
                }
            }

            // =========================
            // REVOGAR PERMISSÃO
            // =========================
            case "revogar" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_PERMISSOES)) {
                    return true;
                }
                if (argumentos.length != 3) {
                    System.out.println("Uso: /usuario <ID> revogar <NOME_PERMISSAO>");
                    return true;
                }
                try {
                    Permissao perm = Permissao.valueOf(argumentos[2].toUpperCase());
                    usuario.revogarPermissao(perm);
                    if (usuarioDAO.atualizarPermissoes(usuario.getIdUsuario(), usuario.getPermissoes())) {
                        System.out.println("Permissão " + perm.name() + " revogada com sucesso de " + usuario.getNomeUsuario() + "!");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Permissão inválida! Digite /usuario " + usuario.getIdUsuario() + " permissoes para ver a lista válida.");
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar permissões no banco: " + e.getMessage());
                }
            }

            // =========================
            // RESET PERMISSOES
            // =========================
            case "reset_permissoes" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_PERMISSOES)) {
                    return true;
                }
                usuario.redefinirParaPadraoDoPerfil();
                try {
                    if (usuarioDAO.atualizarPermissoes(usuario.getIdUsuario(), usuario.getPermissoes())) {
                        System.out.println("Permissões de " + usuario.getNomeUsuario() + " restauradas para o padrão do perfil " + usuario.getPerfil().name() + " com sucesso!");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao restaurar permissões no banco: " + e.getMessage());
                }
            }

            // =========================
            // DESATIVAR / ATIVAR
            // =========================
            case "desativar" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_DESATIVAR)) {
                    return true;
                }
                if (sessao.getUsuarioLogado().getIdUsuario() == usuario.getIdUsuario()) {
                    System.out.println("Erro: Você não pode desativar o seu próprio usuário logado.");
                    return true;
                }
                try {
                    if (usuarioDAO.desativar(usuario.getIdUsuario())) {
                        System.out.println("Usuário " + usuario.getNomeUsuario() + " desativado com sucesso.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao desativar usuário: " + e.getMessage());
                }
            }

            case "ativar" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_DESATIVAR)) {
                    return true;
                }
                try {
                    if (usuarioDAO.ativar(usuario.getIdUsuario())) {
                        System.out.println("Usuário " + usuario.getNomeUsuario() + " reativado com sucesso.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao ativar usuário: " + e.getMessage());
                }
            }

            // =========================
            // DELETAR
            // =========================
            case "deletar" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_DESATIVAR)) {
                    return true;
                }
                if (sessao.getUsuarioLogado().getIdUsuario() == usuario.getIdUsuario()) {
                    System.out.println("Erro: Você não pode deletar o seu próprio usuário logado.");
                    return true;
                }
                try {
                    if (usuarioDAO.deletar(usuario.getIdUsuario())) {
                        System.out.println("Usuário deletado com sucesso.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao deletar usuário: " + e.getMessage());
                }
            }

            default -> System.out.println("Subcomando inválido. Digite /help /usuario para ver os comandos disponíveis.");
        }

        return true;
    }

    private boolean gerenciarUsuarioInterativo(Usuario usuario) {
        if (!sessao.validarPermissao(Permissao.USUARIO_CONSULTAR)) {
            return true;
        }

        Scanner entrada = sessao.getEntrada();
        UsuarioDAO usuarioDAO = sessao.getUsuarioDAO();

        System.out.println("\n==================================================");
        System.out.println("           PAINEL DE GESTÃO DO OPERADOR           ");
        System.out.println("==================================================");
        System.out.printf("Operador: #%d - %s [%s] (%s)%n",
                usuario.getIdUsuario(), usuario.getNomeUsuario(), usuario.getPerfil().name(),
                usuario.isAtivo() ? "ATIVO" : "INATIVO (Bloqueado)");
        System.out.printf("Login: %s | Permissões ativas: %d%n",
                usuario.getLogin(), usuario.getPermissoes().size());
        System.out.println("--------------------------------------------------");
        System.out.println("[1] Consultar dados e permissões detalhadas");
        System.out.println("[2] Alterar nome completo");
        System.out.println("[3] Alterar perfil base (ADMIN, GERENTE, VENDEDOR, CAIXA)");
        System.out.println("[4] Trocar senha de acesso");
        System.out.println("[5] Conceder permissão avulsa");
        System.out.println("[6] Revogar permissão avulsa");
        System.out.println("[7] Restaurar permissões para o padrão do perfil");
        System.out.println("[8] " + (usuario.isAtivo() ? "Desativar operador" : "Ativar operador"));
        System.out.println("[9] Deletar operador permanentemente");
        System.out.println("[0] Concluir / Cancelar");
        System.out.println("--------------------------------------------------");
        System.out.print("Escolha uma opção (0-9 ou 'cancelar'): ");

        String op = entrada.nextLine().trim();
        if (op.isEmpty() || op.equals("0") || op.equalsIgnoreCase("cancelar")) {
            System.out.println("Operação concluída.");
            return true;
        }

        switch (op) {
            case "1" -> {
                System.out.println("\n=== DADOS DO USUÁRIO ===");
                System.out.println("ID:            " + usuario.getIdUsuario());
                System.out.println("Nome:          " + usuario.getNomeUsuario());
                System.out.println("Login:         " + usuario.getLogin());
                System.out.println("Perfil:        " + usuario.getPerfil().name());
                System.out.println("Status:        " + (usuario.isAtivo() ? "ATIVO" : "INATIVO (Bloqueado)"));
                System.out.println("Cadastro em:   " + usuario.getDataCadastro().format(Usuario.FORMATO_DATA_HORA));
                System.out.println("Permissões:    " + usuario.getPermissoes().size() + " ativa(s)");
                Set<Permissao> extras = usuario.getPermissoesAdicionais();
                if (!extras.isEmpty()) {
                    System.out.println("Extras:        " + extras);
                }
                Set<Permissao> revogadas = usuario.getPermissoesRevogadas();
                if (!revogadas.isEmpty()) {
                    System.out.println("Revogadas:     " + revogadas);
                }

                System.out.println("\n=== TODAS AS PERMISSÕES DETALHADAS ===");
                for (Permissao p : Permissao.values()) {
                    boolean ativa = usuario.temPermissao(p);
                    String prefixo = p.isMae() ? "📁 [MÓDULO] " : "  └── 🔹 ";
                    System.out.printf("%s%-32s : %s (%s)%n", prefixo, p.name(),
                            ativa ? "✅ ATIVA" : "❌ INATIVA", p.getDescricao());
                }
            }
            case "2" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_EDITAR)) return true;
                System.out.printf("Nome atual: %s%n", usuario.getNomeUsuario());
                System.out.print("Informe o novo nome completo (ou 'cancelar'): ");
                String novoNome = entrada.nextLine().trim();
                if (novoNome.isEmpty() || novoNome.equalsIgnoreCase("cancelar")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                try {
                    usuario.setNomeUsuario(novoNome);
                    if (usuarioDAO.atualizar(usuario)) {
                        System.out.println("✅ Nome atualizado com sucesso para: " + novoNome);
                    } else {
                        System.out.println("Não foi possível atualizar o nome.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro ao alterar nome: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar usuário no banco: " + e.getMessage());
                }
            }
            case "3" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_EDITAR)) return true;
                System.out.printf("Perfil atual: %s%n", usuario.getPerfil().name());
                System.out.println("Perfis disponíveis: [1] ADMIN, [2] GERENTE, [3] VENDEDOR, [4] CAIXA");
                System.out.print("Escolha o novo perfil (1-4 ou digite o nome / 'cancelar'): ");
                String pStr = entrada.nextLine().trim().toUpperCase();
                if (pStr.isEmpty() || pStr.equalsIgnoreCase("CANCELAR")) {
                    System.out.println("Edição cancelada.");
                    return true;
                }
                PerfilUsuario novoPerfil;
                switch (pStr) {
                    case "1" -> novoPerfil = PerfilUsuario.ADMIN;
                    case "2" -> novoPerfil = PerfilUsuario.GERENTE;
                    case "3" -> novoPerfil = PerfilUsuario.VENDEDOR;
                    case "4" -> novoPerfil = PerfilUsuario.CAIXA;
                    default -> {
                        try {
                            novoPerfil = PerfilUsuario.valueOf(pStr);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Perfil inválido.");
                            return true;
                        }
                    }
                }
                try {
                    usuario.setPerfil(novoPerfil);
                    usuario.redefinirParaPadraoDoPerfil();
                    if (usuarioDAO.atualizar(usuario)) {
                        System.out.println("✅ Perfil alterado para " + novoPerfil.name() + " e permissões redefinidas para o padrão com sucesso!");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar perfil no banco: " + e.getMessage());
                }
            }
            case "4" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_SENHA)) return true;
                String novaSenha = sessao.lerSenha("Digite a nova senha para " + usuario.getNomeUsuario() + ": ");
                if (novaSenha.isEmpty()) {
                    System.out.println("Alteração de senha cancelada.");
                    return true;
                }
                try {
                    if (usuarioDAO.alterarSenha(usuario.getIdUsuario(), novaSenha)) {
                        System.out.println("✅ Senha alterada com sucesso!");
                    } else {
                        System.out.println("Não foi possível alterar a senha.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar senha no banco: " + e.getMessage());
                }
            }
            case "5" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_PERMISSOES)) return true;
                List<Permissao> inativas = new ArrayList<>();
                for (Permissao p : Permissao.values()) {
                    if (!usuario.temPermissao(p)) {
                        inativas.add(p);
                    }
                }
                if (inativas.isEmpty()) {
                    System.out.println("O usuário já possui todas as permissões possíveis ativas.");
                    return true;
                }
                System.out.println("\nPermissões inativas disponíveis para concessão:");
                for (int i = 0; i < inativas.size(); i++) {
                    Permissao p = inativas.get(i);
                    System.out.printf("[%2d] %-30s (%s)%n", (i + 1), p.name(), p.getDescricao());
                }
                System.out.print("Escolha o número da permissão a conceder (ou 'cancelar'): ");
                String sel = entrada.nextLine().trim();
                if (sel.isEmpty() || sel.equalsIgnoreCase("cancelar")) {
                    System.out.println("Operação cancelada.");
                    return true;
                }
                Permissao perm = null;
                try {
                    int idx = Integer.parseInt(sel);
                    if (idx >= 1 && idx <= inativas.size()) {
                        perm = inativas.get(idx - 1);
                    }
                } catch (NumberFormatException e) {
                    try {
                        perm = Permissao.valueOf(sel.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (perm == null) {
                    System.out.println("Permissão inválida.");
                    return true;
                }
                try {
                    usuario.concederPermissao(perm);
                    if (usuarioDAO.atualizarPermissoes(usuario.getIdUsuario(), usuario.getPermissoes())) {
                        System.out.println("✅ Permissão " + perm.name() + " concedida com sucesso a " + usuario.getNomeUsuario() + "!");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar permissões no banco: " + e.getMessage());
                }
            }
            case "6" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_PERMISSOES)) return true;
                List<Permissao> ativas = new ArrayList<>(usuario.getPermissoes());
                if (ativas.isEmpty()) {
                    System.out.println("O usuário não possui permissões ativas para revogar.");
                    return true;
                }
                System.out.println("\nPermissões ativas disponíveis para revogação:");
                for (int i = 0; i < ativas.size(); i++) {
                    Permissao p = ativas.get(i);
                    System.out.printf("[%2d] %-30s (%s)%n", (i + 1), p.name(), p.getDescricao());
                }
                System.out.print("Escolha o número da permissão a revogar (ou 'cancelar'): ");
                String sel = entrada.nextLine().trim();
                if (sel.isEmpty() || sel.equalsIgnoreCase("cancelar")) {
                    System.out.println("Operação cancelada.");
                    return true;
                }
                Permissao perm = null;
                try {
                    int idx = Integer.parseInt(sel);
                    if (idx >= 1 && idx <= ativas.size()) {
                        perm = ativas.get(idx - 1);
                    }
                } catch (NumberFormatException e) {
                    try {
                        perm = Permissao.valueOf(sel.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (perm == null) {
                    System.out.println("Permissão inválida.");
                    return true;
                }
                try {
                    usuario.revogarPermissao(perm);
                    if (usuarioDAO.atualizarPermissoes(usuario.getIdUsuario(), usuario.getPermissoes())) {
                        System.out.println("✅ Permissão " + perm.name() + " revogada com sucesso de " + usuario.getNomeUsuario() + "!");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao atualizar permissões no banco: " + e.getMessage());
                }
            }
            case "7" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_ALTERAR_PERMISSOES)) return true;
                usuario.redefinirParaPadraoDoPerfil();
                try {
                    if (usuarioDAO.atualizarPermissoes(usuario.getIdUsuario(), usuario.getPermissoes())) {
                        System.out.println("✅ Permissões de " + usuario.getNomeUsuario() + " restauradas para o padrão do perfil " + usuario.getPerfil().name() + " com sucesso!");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao restaurar permissões no banco: " + e.getMessage());
                }
            }
            case "8" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_DESATIVAR)) return true;
                if (sessao.getUsuarioLogado().getIdUsuario() == usuario.getIdUsuario()) {
                    System.out.println("Erro: Você não pode alterar o status do seu próprio usuário logado.");
                    return true;
                }
                if (usuario.isAtivo()) {
                    System.out.printf("Tem certeza que deseja desativar o operador %s? (s/n): ", usuario.getNomeUsuario());
                    String conf = entrada.nextLine().trim().toLowerCase();
                    if (!conf.equals("s") && !conf.equals("sim")) {
                        System.out.println("Operação cancelada.");
                        return true;
                    }
                    try {
                        if (usuarioDAO.desativar(usuario.getIdUsuario())) {
                            System.out.println("✅ Usuário " + usuario.getNomeUsuario() + " desativado com sucesso.");
                        }
                    } catch (SQLException e) {
                        System.out.println("Erro ao desativar usuário: " + e.getMessage());
                    }
                } else {
                    try {
                        if (usuarioDAO.ativar(usuario.getIdUsuario())) {
                            System.out.println("✅ Usuário " + usuario.getNomeUsuario() + " reativado com sucesso.");
                        }
                    } catch (SQLException e) {
                        System.out.println("Erro ao ativar usuário: " + e.getMessage());
                    }
                }
            }
            case "9" -> {
                if (!sessao.validarPermissao(Permissao.USUARIO_DESATIVAR)) return true;
                if (sessao.getUsuarioLogado().getIdUsuario() == usuario.getIdUsuario()) {
                    System.out.println("Erro: Você não pode deletar o seu próprio usuário logado.");
                    return true;
                }
                System.out.printf("⚠️ Tem certeza que deseja deletar o operador #%d - %s permanentemente? (s/n): ",
                        usuario.getIdUsuario(), usuario.getNomeUsuario());
                String conf = entrada.nextLine().trim().toLowerCase();
                if (!conf.equals("s") && !conf.equals("sim")) {
                    System.out.println("Exclusão cancelada.");
                    return true;
                }
                try {
                    if (usuarioDAO.deletar(usuario.getIdUsuario())) {
                        System.out.println("✅ Usuário deletado com sucesso.");
                    } else {
                        System.out.println("Não foi possível deletar o usuário.");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao deletar usuário: " + e.getMessage());
                }
            }
            default -> System.out.println("Opção inválida.");
        }

        return true;
    }
}
