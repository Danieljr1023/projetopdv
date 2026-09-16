package projetopdv.dados;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import projetopdv.seguranca.CriptografiaUtil;
import projetopdv.usuario.PerfilUsuario;
import projetopdv.usuario.Permissao;
import projetopdv.usuario.Usuario;

public final class BancoDeDados {

    private BancoDeDados() {
        // Classe utilitária com métodos estáticos; não deve ser instanciada.
    }

    // Testa conexão com SQLite e cria arquivo projetopdv.db
    private static final String URL = "jdbc:sqlite:projetopdv.db";

    public static Connection conectar() throws SQLException {
        Connection conexao = DriverManager.getConnection(URL);
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conexao;
    }

    public static void criarTabelaProduto() {
        String sql = """
            CREATE TABLE IF NOT EXISTS produto (
                id_produto INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo_barras TEXT NOT NULL,
                nome_produto TEXT NOT NULL,
                preco_centavos INTEGER NOT NULL
            );
            """;

        try (Connection conexaoProduto = conectar(); Statement comando = conexaoProduto.createStatement()) {

            comando.execute(sql);

            System.out.println("Tabela produto criada/verificada com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela produto:");
            System.out.println(e.getMessage());
        }
    }

    public static void criarTabelaCliente() {
        String sql = """
            CREATE TABLE IF NOT EXISTS cliente (
                id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
                nome_cliente TEXT NOT NULL,
                data_nascimento TEXT NOT NULL,
                cpf TEXT NOT NULL
            );
            """;

        try (Connection conexaoCliente = conectar(); Statement comando = conexaoCliente.createStatement()) {

            comando.execute(sql);

            System.out.println("Tabela cliente criada/verificada com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela cliente:");
            System.out.println(e.getMessage());
        }
    }

    public static void criarTabelaOrcamento() {
        String sql = """
            CREATE TABLE IF NOT EXISTS orcamento (
                id_orcamento INTEGER PRIMARY KEY AUTOINCREMENT,
                nome_orcamento TEXT,
                status_orcamento TEXT NOT NULL DEFAULT 'ABERTO',
                data_orcamento TEXT NOT NULL,
                valor_total_centavos INTEGER NOT NULL,
                cliente_id INTEGER,
                FOREIGN KEY (cliente_id) REFERENCES cliente(id_cliente)
            );
            """;

        try (Connection conexaoOrcamento = conectar(); Statement comando = conexaoOrcamento.createStatement()) {

            comando.execute(sql);

            System.out.println("Tabela orcamento criada/verificada com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela orcamento:");
            System.out.println(e.getMessage());
        }
    }

    public static void criarTabelaItemOrcamento() {
        String sql = """
            CREATE TABLE IF NOT EXISTS item_orcamento (
                orcamento_id INTEGER NOT NULL,
                numero_item INTEGER NOT NULL,
                produto_id INTEGER,
                nome_produto TEXT NOT NULL,
                codigo_barras TEXT,
                quantidade INTEGER NOT NULL,
                preco_unitario_tabela_centavos INTEGER NOT NULL DEFAULT 0,
                preco_unitario_centavos INTEGER NOT NULL,
                PRIMARY KEY (orcamento_id, numero_item),
                FOREIGN KEY (orcamento_id) REFERENCES orcamento(id_orcamento) ON DELETE CASCADE,
                FOREIGN KEY (produto_id) REFERENCES produto(id_produto) ON DELETE SET NULL
            );
            """;

        try (Connection conexaoItem = conectar(); Statement comando = conexaoItem.createStatement()) {

            comando.execute(sql);

            // Migração defensiva caso a tabela já existisse sem o campo de preço de tabela
            try {
                comando.execute("ALTER TABLE item_orcamento ADD COLUMN preco_unitario_tabela_centavos INTEGER NOT NULL DEFAULT 0;");
            } catch (SQLException ignored) {
            }

            System.out.println("Tabela item_orcamento criada/verificada com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela item_orcamento:");
            System.out.println(e.getMessage());
        }
    }

    public static void criarTabelaUsuario() {
        String sql = """
            CREATE TABLE IF NOT EXISTS usuario (
                id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                nome_usuario TEXT NOT NULL,
                login TEXT NOT NULL UNIQUE,
                senha_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                perfil TEXT NOT NULL,
                ativo INTEGER NOT NULL DEFAULT 1,
                data_cadastro TEXT NOT NULL
            );
            """;

        try (Connection conexaoUsuario = conectar(); Statement comando = conexaoUsuario.createStatement()) {

            comando.execute(sql);

            System.out.println("Tabela usuario criada/verificada com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela usuario:");
            System.out.println(e.getMessage());
        }
    }

    public static void criarTabelaUsuarioPermissao() {
        String sql = """
            CREATE TABLE IF NOT EXISTS usuario_permissao (
                usuario_id INTEGER NOT NULL,
                permissao TEXT NOT NULL,
                PRIMARY KEY (usuario_id, permissao),
                FOREIGN KEY (usuario_id) REFERENCES usuario(id_usuario) ON DELETE CASCADE
            );
            """;

        try (Connection conexaoPermissao = conectar(); Statement comando = conexaoPermissao.createStatement()) {

            comando.execute(sql);

            System.out.println("Tabela usuario_permissao criada/verificada com sucesso!");

            inicializarUsuarioAdminPadrao(conexaoPermissao);

        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela usuario_permissao:");
            System.out.println(e.getMessage());
        }
    }

    private static void inicializarUsuarioAdminPadrao(Connection conexao) {
        String sqlVerifica = "SELECT COUNT(*) FROM usuario;";

        try (Statement stmt = conexao.createStatement(); ResultSet rs = stmt.executeQuery(sqlVerifica)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String salt = CriptografiaUtil.gerarSalt();
                String hash = CriptografiaUtil.gerarHash("admin123", salt);
                String dataCadastro = LocalDateTime.now().format(Usuario.FORMATO_DATA_HORA);

                String sqlInsertUsuario = """
                    INSERT INTO usuario (nome_usuario, login, senha_hash, salt, perfil, ativo, data_cadastro)
                    VALUES (?, ?, ?, ?, ?, ?, ?);
                    """;

                try (PreparedStatement psUser = conexao.prepareStatement(sqlInsertUsuario, Statement.RETURN_GENERATED_KEYS)) {
                    psUser.setString(1, "Administrador do Sistema");
                    psUser.setString(2, "admin");
                    psUser.setString(3, hash);
                    psUser.setString(4, salt);
                    psUser.setString(5, PerfilUsuario.ADMIN.name());
                    psUser.setInt(6, 1);
                    psUser.setString(7, dataCadastro);
                    psUser.executeUpdate();

                    try (ResultSet chaves = psUser.getGeneratedKeys()) {
                        if (chaves.next()) {
                            int idAdmin = chaves.getInt(1);

                            String sqlInsertPermissao = "INSERT INTO usuario_permissao (usuario_id, permissao) VALUES (?, ?);";
                            try (PreparedStatement psPerm = conexao.prepareStatement(sqlInsertPermissao)) {
                                for (Permissao perm : PerfilUsuario.ADMIN.getPermissoesPadrao()) {
                                    psPerm.setInt(1, idAdmin);
                                    psPerm.setString(2, perm.name());
                                    psPerm.addBatch();
                                }
                                psPerm.executeBatch();
                            }
                            System.out.println("Usuário padrão 'admin' criado com sucesso com perfil ADMIN!");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao inicializar usuário admin padrão:");
            System.out.println(e.getMessage());
        }
    }

    public static void criarTabelaVenda() {
        String sql = """
            CREATE TABLE IF NOT EXISTS venda (
                id_venda INTEGER PRIMARY KEY AUTOINCREMENT,
                orcamento_id INTEGER NOT NULL UNIQUE,
                data_venda TEXT NOT NULL,
                valor_total_centavos INTEGER NOT NULL,
                forma_pagamento TEXT NOT NULL,
                status_venda TEXT NOT NULL DEFAULT 'CONCLUIDA',
                modalidade_estorno TEXT,
                data_estorno TEXT,
                motivo_estorno TEXT,
                FOREIGN KEY (orcamento_id) REFERENCES orcamento(id_orcamento)
            );
            """;

        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(sql);
            System.out.println("Tabela venda criada/verificada com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela venda: " + e.getMessage());
        }
    }

    public static void criarTabelaItemVenda() {
        String sql = """
            CREATE TABLE IF NOT EXISTS item_venda (
                id_item_venda INTEGER PRIMARY KEY AUTOINCREMENT,
                venda_id INTEGER NOT NULL,
                numero_item INTEGER NOT NULL,
                numero_item_orcamento INTEGER,
                produto_id INTEGER,
                nome_produto TEXT NOT NULL,
                codigo_barras TEXT,
                quantidade INTEGER NOT NULL,
                preco_unitario_tabela_centavos INTEGER NOT NULL,
                preco_unitario_centavos INTEGER NOT NULL,
                FOREIGN KEY (venda_id) REFERENCES venda(id_venda) ON DELETE CASCADE,
                FOREIGN KEY (produto_id) REFERENCES produto(id_produto) ON DELETE SET NULL
            );
            """;

        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(sql);
            System.out.println("Tabela item_venda criada/verificada com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela item_venda: " + e.getMessage());
        }
    }

    public static void criarTabelaValeCompra() {
        String sql = """
            CREATE TABLE IF NOT EXISTS vale_compra (
                id_vale_compra INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo_vale TEXT NOT NULL UNIQUE,
                cliente_id INTEGER,
                venda_origem_id INTEGER,
                valor_original_centavos INTEGER NOT NULL,
                saldo_centavos INTEGER NOT NULL,
                data_emissao TEXT NOT NULL,
                data_validade TEXT,
                status_vale TEXT NOT NULL DEFAULT 'ATIVO',
                observacoes TEXT,
                FOREIGN KEY (cliente_id) REFERENCES cliente(id_cliente) ON DELETE SET NULL,
                FOREIGN KEY (venda_origem_id) REFERENCES venda(id_venda) ON DELETE SET NULL
            );
            """;

        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(sql);
            System.out.println("Tabela vale_compra criada/verificada com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela vale_compra: " + e.getMessage());
        }
    }

    public static void criarTabelaDevolucaoItem() {
        String sql = """
            CREATE TABLE IF NOT EXISTS devolucao_item (
                id_devolucao INTEGER PRIMARY KEY AUTOINCREMENT,
                venda_id INTEGER NOT NULL,
                item_venda_id INTEGER NOT NULL,
                quantidade_devolvida INTEGER NOT NULL,
                valor_unitario_centavos INTEGER NOT NULL,
                valor_total_centavos INTEGER NOT NULL,
                motivo TEXT NOT NULL,
                data_devolucao TEXT NOT NULL,
                vale_compra_id INTEGER,
                FOREIGN KEY (venda_id) REFERENCES venda(id_venda) ON DELETE CASCADE,
                FOREIGN KEY (item_venda_id) REFERENCES item_venda(id_item_venda) ON DELETE CASCADE,
                FOREIGN KEY (vale_compra_id) REFERENCES vale_compra(id_vale_compra) ON DELETE SET NULL
            );
            """;

        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(sql);
            System.out.println("Tabela devolucao_item criada/verificada com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela devolucao_item: " + e.getMessage());
        }
    }

    public static void criarTabelaMovimentacaoCaixa() {
        String sql = """
            CREATE TABLE IF NOT EXISTS movimentacao_caixa (
                id_movimentacao INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo_movimentacao TEXT NOT NULL,
                valor_centavos INTEGER NOT NULL,
                data_hora TEXT NOT NULL,
                vale_compra_id INTEGER,
                venda_id INTEGER,
                justificativa TEXT NOT NULL,
                operador_id INTEGER,
                FOREIGN KEY (vale_compra_id) REFERENCES vale_compra(id_vale_compra) ON DELETE SET NULL,
                FOREIGN KEY (venda_id) REFERENCES venda(id_venda) ON DELETE SET NULL,
                FOREIGN KEY (operador_id) REFERENCES usuario(id_usuario) ON DELETE SET NULL
            );
            """;

        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(sql);
            System.out.println("Tabela movimentacao_caixa criada/verificada com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela movimentacao_caixa: " + e.getMessage());
        }
    }

    public static void resetarTudo() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("PRAGMA foreign_keys = OFF;");
            comando.executeUpdate("DELETE FROM movimentacao_caixa;");
            comando.executeUpdate("DELETE FROM devolucao_item;");
            comando.executeUpdate("DELETE FROM vale_compra;");
            comando.executeUpdate("DELETE FROM item_venda;");
            comando.executeUpdate("DELETE FROM venda;");
            comando.executeUpdate("DELETE FROM usuario_permissao;");
            comando.executeUpdate("DELETE FROM usuario;");
            comando.executeUpdate("DELETE FROM item_orcamento;");
            comando.executeUpdate("DELETE FROM orcamento;");
            comando.executeUpdate("DELETE FROM cliente;");
            comando.executeUpdate("DELETE FROM produto;");
            comando.executeUpdate("DELETE FROM sqlite_sequence;");
            comando.executeUpdate("PRAGMA foreign_keys = ON;");
            inicializarUsuarioAdminPadrao(conexao);
            comando.executeUpdate("VACUUM;");
        }
    }

    public static void resetarTabelaProduto() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("PRAGMA foreign_keys = OFF;");
            comando.executeUpdate("UPDATE item_orcamento SET produto_id = NULL;");
            comando.executeUpdate("UPDATE item_venda SET produto_id = NULL;");
            comando.executeUpdate("DELETE FROM produto;");
            comando.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'produto';");
            comando.executeUpdate("PRAGMA foreign_keys = ON;");
            comando.executeUpdate("VACUUM;");
        }
    }

    public static void resetarTabelaCliente() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("PRAGMA foreign_keys = OFF;");
            comando.executeUpdate("UPDATE orcamento SET cliente_id = NULL;");
            comando.executeUpdate("UPDATE vale_compra SET cliente_id = NULL;");
            comando.executeUpdate("DELETE FROM cliente;");
            comando.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'cliente';");
            comando.executeUpdate("PRAGMA foreign_keys = ON;");
            comando.executeUpdate("VACUUM;");
        }
    }

    public static void resetarTabelaOrcamento() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("PRAGMA foreign_keys = OFF;");
            comando.executeUpdate("DELETE FROM item_orcamento;");
            comando.executeUpdate("DELETE FROM orcamento;");
            comando.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('orcamento', 'item_orcamento');");
            comando.executeUpdate("PRAGMA foreign_keys = ON;");
            comando.executeUpdate("VACUUM;");
        }
    }

    public static void resetarTabelaVenda() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("PRAGMA foreign_keys = OFF;");
            comando.executeUpdate("DELETE FROM movimentacao_caixa;");
            comando.executeUpdate("DELETE FROM devolucao_item;");
            comando.executeUpdate("DELETE FROM vale_compra;");
            comando.executeUpdate("DELETE FROM item_venda;");
            comando.executeUpdate("DELETE FROM venda;");
            comando.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('venda', 'item_venda', 'devolucao_item', 'vale_compra', 'movimentacao_caixa');");
            comando.executeUpdate("PRAGMA foreign_keys = ON;");
            comando.executeUpdate("VACUUM;");
        }
    }

    public static void resetarTabelaUsuario() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.executeUpdate("PRAGMA foreign_keys = OFF;");
            comando.executeUpdate("DELETE FROM usuario_permissao;");
            comando.executeUpdate("DELETE FROM usuario;");
            comando.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'usuario';");
            comando.executeUpdate("PRAGMA foreign_keys = ON;");
            inicializarUsuarioAdminPadrao(conexao);
            comando.executeUpdate("VACUUM;");
        }
    }

    public static void resetarBanco() {
        try {
            resetarTudo();
            System.out.println("Banco de dados resetado com sucesso! Tabelas limpas e contadores de ID zerados.");
        } catch (SQLException e) {
            System.out.println("Erro ao resetar banco de dados:");
            System.out.println(e.getMessage());
        }
    }

    public static void inicializarTabelas() {
        criarTabelaProduto();
        criarTabelaCliente();
        criarTabelaOrcamento();
        criarTabelaItemOrcamento();
        criarTabelaUsuario();
        criarTabelaUsuarioPermissao();
        criarTabelaVenda();
        criarTabelaItemVenda();
        criarTabelaValeCompra();
        criarTabelaDevolucaoItem();
        criarTabelaMovimentacaoCaixa();
    }

    public static void main(String[] args) {
        inicializarTabelas();
    }
}
