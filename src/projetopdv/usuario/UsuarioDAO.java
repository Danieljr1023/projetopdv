package projetopdv.usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import projetopdv.dados.BancoDeDados;
import projetopdv.seguranca.CriptografiaUtil;

public class UsuarioDAO {

    private static final DateTimeFormatter FORMATO_DATA_HORA = Usuario.FORMATO_DATA_HORA;
    private static final DateTimeFormatter FORMATO_DATA_HORA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime parseDataCadastro(String dataStr) {
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String tratada = dataStr.trim();
        try {
            return LocalDateTime.parse(tratada, FORMATO_DATA_HORA);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(tratada, FORMATO_DATA_HORA_ISO);
            } catch (Exception ex) {
                try {
                    return LocalDateTime.parse(tratada);
                } catch (Exception ex2) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    public Usuario cadastrar(String nomeUsuario, String login, String senhaPlana, PerfilUsuario perfil) throws SQLException {
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil do usuário não pode ser nulo.");
        }
        return cadastrar(nomeUsuario, login, senhaPlana, perfil, perfil.getPermissoesPadrao());
    }

    public Usuario cadastrar(
            String nomeUsuario,
            String login,
            String senhaPlana,
            PerfilUsuario perfil,
            Set<Permissao> permissoesIniciais
    ) throws SQLException {
        if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do usuário não pode ser vazio.");
        }
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Login do usuário não pode ser vazio.");
        }
        if (senhaPlana == null || senhaPlana.trim().isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser vazia.");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil do usuário não pode ser nulo.");
        }

        String nomeTratado = nomeUsuario.trim();
        String loginTratado = login.trim();
        String salt = CriptografiaUtil.gerarSalt();
        String senhaHash = CriptografiaUtil.gerarHash(senhaPlana, salt);
        LocalDateTime agora = LocalDateTime.now();
        String dataFormatada = agora.format(FORMATO_DATA_HORA);

        Set<Permissao> permissoesEfetivas = EnumSet.noneOf(Permissao.class);
        if (permissoesIniciais == null || permissoesIniciais.isEmpty()) {
            permissoesEfetivas.addAll(perfil.getPermissoesPadrao());
        } else {
            permissoesEfetivas.addAll(permissoesIniciais);
        }

        String sqlUsuario = """
            INSERT INTO usuario (nome_usuario, login, senha_hash, salt, perfil, ativo, data_cadastro)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        String sqlPermissao = "INSERT INTO usuario_permissao (usuario_id, permissao) VALUES (?, ?);";

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            int idUsuarioGerado;
            try (PreparedStatement psUser = conexao.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, nomeTratado);
                psUser.setString(2, loginTratado);
                psUser.setString(3, senhaHash);
                psUser.setString(4, salt);
                psUser.setString(5, perfil.name());
                psUser.setInt(6, 1);
                psUser.setString(7, dataFormatada);

                psUser.executeUpdate();

                try (ResultSet chaves = psUser.getGeneratedKeys()) {
                    if (chaves.next()) {
                        idUsuarioGerado = chaves.getInt(1);
                    } else {
                        conexao.rollback();
                        throw new SQLException("Falha ao obter o ID gerado para o usuário.");
                    }
                }
            }

            try (PreparedStatement psPerm = conexao.prepareStatement(sqlPermissao)) {
                for (Permissao perm : permissoesEfetivas) {
                    psPerm.setInt(1, idUsuarioGerado);
                    psPerm.setString(2, perm.name());
                    psPerm.addBatch();
                }
                psPerm.executeBatch();
            }

            conexao.commit();

            return new Usuario(
                    idUsuarioGerado,
                    nomeTratado,
                    loginTratado,
                    senhaHash,
                    salt,
                    perfil,
                    true,
                    agora,
                    permissoesEfetivas
            );

        } catch (SQLException e) {
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException ex) {
                    System.out.println("Erro ao realizar rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conexao != null) {
                try {
                    conexao.setAutoCommit(true);
                    conexao.close();
                } catch (SQLException ex) {
                    System.out.println("Erro ao fechar conexão: " + ex.getMessage());
                }
            }
        }
    }

    public Usuario autenticar(String login, String senhaPlana) throws SQLException {
        if (login == null || senhaPlana == null) {
            return null;
        }

        Usuario usuario = buscarPorLogin(login.trim());
        if (usuario == null || !usuario.isAtivo()) {
            return null;
        }

        boolean senhaValida = CriptografiaUtil.verificarSenha(senhaPlana, usuario.getSalt(), usuario.getSenhaHash());
        if (senhaValida) {
            return usuario;
        }

        return null;
    }

    public Usuario buscarPorId(int idUsuario) throws SQLException {
        String sql = """
            SELECT id_usuario, nome_usuario, login, senha_hash, salt, perfil, ativo, data_cadastro
            FROM usuario
            WHERE id_usuario = ?;
            """;

        try (Connection conexao = BancoDeDados.conectar(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs, conexao);
                }
            }
        }
        return null;
    }

    public Usuario buscarPorLogin(String login) throws SQLException {
        if (login == null || login.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT id_usuario, nome_usuario, login, senha_hash, salt, perfil, ativo, data_cadastro
            FROM usuario
            WHERE login = ?;
            """;

        try (Connection conexao = BancoDeDados.conectar(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs, conexao);
                }
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = """
            SELECT id_usuario, nome_usuario, login, senha_hash, salt, perfil, ativo, data_cadastro
            FROM usuario
            ORDER BY id_usuario ASC;
            """;

        try (Connection conexao = BancoDeDados.conectar(); Statement stmt = conexao.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs, conexao));
            }
        }
        return usuarios;
    }

    public List<Usuario> pesquisar(String termo) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String termoTratado = (termo != null) ? "%" + termo.trim() + "%" : "%";

        String sql = """
            SELECT id_usuario, nome_usuario, login, senha_hash, salt, perfil, ativo, data_cadastro
            FROM usuario
            WHERE nome_usuario LIKE ? OR login LIKE ?
            ORDER BY id_usuario ASC;
            """;

        try (Connection conexao = BancoDeDados.conectar(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, termoTratado);
            ps.setString(2, termoTratado);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapearUsuario(rs, conexao));
                }
            }
        }
        return usuarios;
    }

    public boolean atualizar(Usuario usuario) throws SQLException {
        if (usuario == null) {
            return false;
        }

        String sqlUsuario = """
            UPDATE usuario
            SET nome_usuario = ?,
                login = ?,
                perfil = ?,
                ativo = ?
            WHERE id_usuario = ?;
            """;

        String sqlDeletarPermissoes = "DELETE FROM usuario_permissao WHERE usuario_id = ?;";
        String sqlInserirPermissao = "INSERT INTO usuario_permissao (usuario_id, permissao) VALUES (?, ?);";

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            try (PreparedStatement psUser = conexao.prepareStatement(sqlUsuario)) {
                psUser.setString(1, usuario.getNomeUsuario());
                psUser.setString(2, usuario.getLogin());
                psUser.setString(3, usuario.getPerfil().name());
                psUser.setInt(4, usuario.isAtivo() ? 1 : 0);
                psUser.setInt(5, usuario.getIdUsuario());

                int linhas = psUser.executeUpdate();
                if (linhas == 0) {
                    conexao.rollback();
                    return false;
                }
            }

            try (PreparedStatement psDel = conexao.prepareStatement(sqlDeletarPermissoes)) {
                psDel.setInt(1, usuario.getIdUsuario());
                psDel.executeUpdate();
            }

            try (PreparedStatement psIns = conexao.prepareStatement(sqlInserirPermissao)) {
                for (Permissao perm : usuario.getPermissoes()) {
                    psIns.setInt(1, usuario.getIdUsuario());
                    psIns.setString(2, perm.name());
                    psIns.addBatch();
                }
                psIns.executeBatch();
            }

            conexao.commit();
            return true;

        } catch (SQLException e) {
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException ex) {
                    System.out.println("Erro ao realizar rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conexao != null) {
                try {
                    conexao.setAutoCommit(true);
                    conexao.close();
                } catch (SQLException ex) {
                    System.out.println("Erro ao fechar conexão: " + ex.getMessage());
                }
            }
        }
    }

    public boolean alterarSenha(int idUsuario, String novaSenhaPlana) throws SQLException {
        if (idUsuario <= 0 || novaSenhaPlana == null || novaSenhaPlana.trim().isEmpty()) {
            throw new IllegalArgumentException("ID do usuário ou senha inválidos.");
        }

        String salt = CriptografiaUtil.gerarSalt();
        String hash = CriptografiaUtil.gerarHash(novaSenhaPlana, salt);

        String sql = """
            UPDATE usuario
            SET senha_hash = ?, salt = ?
            WHERE id_usuario = ?;
            """;

        try (Connection conexao = BancoDeDados.conectar(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, idUsuario);

            int linhas = ps.executeUpdate();
            return linhas > 0;
        }
    }

    public boolean atualizarPermissoes(int idUsuario, Set<Permissao> novasPermissoes) throws SQLException {
        if (idUsuario <= 0) {
            return false;
        }

        String sqlDeletar = "DELETE FROM usuario_permissao WHERE usuario_id = ?;";
        String sqlInserir = "INSERT INTO usuario_permissao (usuario_id, permissao) VALUES (?, ?);";

        Connection conexao = null;
        try {
            conexao = BancoDeDados.conectar();
            conexao.setAutoCommit(false);

            try (PreparedStatement psDel = conexao.prepareStatement(sqlDeletar)) {
                psDel.setInt(1, idUsuario);
                psDel.executeUpdate();
            }

            if (novasPermissoes != null && !novasPermissoes.isEmpty()) {
                try (PreparedStatement psIns = conexao.prepareStatement(sqlInserir)) {
                    for (Permissao perm : novasPermissoes) {
                        psIns.setInt(1, idUsuario);
                        psIns.setString(2, perm.name());
                        psIns.addBatch();
                    }
                    psIns.executeBatch();
                }
            }

            conexao.commit();
            return true;

        } catch (SQLException e) {
            if (conexao != null) {
                try {
                    conexao.rollback();
                } catch (SQLException ex) {
                    System.out.println("Erro ao realizar rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conexao != null) {
                try {
                    conexao.setAutoCommit(true);
                    conexao.close();
                } catch (SQLException ex) {
                    System.out.println("Erro ao fechar conexão: " + ex.getMessage());
                }
            }
        }
    }

    public boolean setAtivo(int idUsuario, boolean ativo) throws SQLException {
        String sql = "UPDATE usuario SET ativo = ? WHERE id_usuario = ?;";
        try (Connection conexao = BancoDeDados.conectar(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, ativo ? 1 : 0);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean desativar(int idUsuario) throws SQLException {
        return setAtivo(idUsuario, false);
    }

    public boolean ativar(int idUsuario) throws SQLException {
        return setAtivo(idUsuario, true);
    }

    public boolean deletar(int idUsuario) throws SQLException {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?;";
        try (Connection conexao = BancoDeDados.conectar(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    private Set<Permissao> carregarPermissoes(int idUsuario, Connection conexao) throws SQLException {
        Set<Permissao> permissoes = EnumSet.noneOf(Permissao.class);
        String sql = "SELECT permissao FROM usuario_permissao WHERE usuario_id = ?;";

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomePermissao = rs.getString("permissao");
                    try {
                        permissoes.add(Permissao.valueOf(nomePermissao));
                    } catch (IllegalArgumentException ignored) {
                        // Ignora valores obsoletos defensivamente
                    }
                }
            }
        }
        return permissoes;
    }

    private Usuario mapearUsuario(ResultSet rs, Connection conexao) throws SQLException {
        int id = rs.getInt("id_usuario");
        String nome = rs.getString("nome_usuario");
        String login = rs.getString("login");
        String senhaHash = rs.getString("senha_hash");
        String salt = rs.getString("salt");
        String perfilStr = rs.getString("perfil");
        boolean ativo = rs.getInt("ativo") == 1;
        String dataCadastroStr = rs.getString("data_cadastro");

        PerfilUsuario perfil;
        try {
            perfil = PerfilUsuario.valueOf(perfilStr);
        } catch (Exception e) {
            perfil = PerfilUsuario.CAIXA;
        }

        Set<Permissao> permissoes = carregarPermissoes(id, conexao);

        return new Usuario(
                id,
                nome,
                login,
                senhaHash,
                salt,
                perfil,
                ativo,
                parseDataCadastro(dataCadastroStr),
                permissoes
        );
    }
}
