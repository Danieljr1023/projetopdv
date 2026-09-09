package projetopdv.cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import projetopdv.dados.BancoDeDados;

public class ClienteDAO {

    private static final java.time.format.DateTimeFormatter FORMATO_DATA_BR = Cliente.FORMATO_DATA;
    private static final java.time.format.DateTimeFormatter FORMATO_DATA_ISO = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate parseDataNascimento(String dataNascimento) {
        if (dataNascimento == null || dataNascimento.trim().isEmpty()) {
            return null;
        }
        String tratada = dataNascimento.trim();
        try {
            return LocalDate.parse(tratada, FORMATO_DATA_BR);
        } catch (Exception e) {
            try {
                return LocalDate.parse(tratada, FORMATO_DATA_ISO);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public Cliente cadastrar(
            String nomeCliente,
            String dataNascimento,
            String cpf) {

        String sql = """
            INSERT INTO cliente
                (nome_cliente, data_nascimento, cpf)
            VALUES (?, ?, ?);
            """;

        try (
                Connection conexaoCliente = BancoDeDados.conectar();
                PreparedStatement comando = conexaoCliente.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            comando.setString(1, nomeCliente);
            comando.setString(2, dataNascimento);
            comando.setString(3, cpf);

            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {

                if (chaves.next()) {

                    int idCliente = chaves.getInt(1);

                    return new Cliente(
                            idCliente,
                            nomeCliente,
                            parseDataNascimento(dataNascimento),
                            cpf
                    );
                }
            }

            throw new SQLException(
                    "O banco não retornou o ID do cliente."
            );

        } catch (SQLException e) {

            System.out.println("Erro ao cadastrar cliente:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public Cliente buscarPorId(int idCliente) {

        String sql = """
            SELECT
                id_cliente,
                nome_cliente,
                data_nascimento,
                cpf
            FROM cliente
            WHERE id_cliente = ?;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setInt(1, idCliente);

            try (ResultSet resultado = comando.executeQuery()) {

                if (resultado.next()) {

                    int id = resultado.getInt("id_cliente");
                    String nome = resultado.getString("nome_cliente");
                    String dataNascimento = resultado.getString("data_nascimento");
                    String cpf = resultado.getString("cpf");

                    return new Cliente(
                            id,
                            nome,
                            parseDataNascimento(dataNascimento),
                            cpf
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println("Erro ao consultar Cliente:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public Cliente buscarPorCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT
                id_cliente,
                nome_cliente,
                data_nascimento,
                cpf
            FROM cliente
            WHERE cpf = ?;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setString(1, cpf.trim());

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    int id = resultado.getInt("id_cliente");
                    String nome = resultado.getString("nome_cliente");
                    String dataNascimento = resultado.getString("data_nascimento");
                    String cpfBanco = resultado.getString("cpf");

                    return new Cliente(
                            id,
                            nome,
                            parseDataNascimento(dataNascimento),
                            cpfBanco
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar cliente por CPF:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public List<Cliente> pesquisarPorTermo(String termo) {
        List<Cliente> clientes = new ArrayList<>();
        if (termo == null || termo.trim().isEmpty()) {
            return clientes;
        }

        String termoTratado = termo.trim();
        String padraoLike;
        if (termoTratado.contains("%")) {
            padraoLike = termoTratado.endsWith("%") ? termoTratado : termoTratado + "%";
        } else {
            padraoLike = "%" + termoTratado + "%";
        }

        String sql = """
            SELECT
                id_cliente,
                nome_cliente,
                data_nascimento,
                cpf
            FROM cliente
            WHERE LOWER(nome_cliente) LIKE LOWER(?) OR cpf LIKE ?
            ORDER BY nome_cliente;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {
            comando.setString(1, padraoLike);
            comando.setString(2, padraoLike);

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    int id = resultado.getInt("id_cliente");
                    String nome = resultado.getString("nome_cliente");
                    String dataNascimento = resultado.getString("data_nascimento");
                    String cpf = resultado.getString("cpf");

                    clientes.add(new Cliente(id, nome, parseDataNascimento(dataNascimento), cpf));
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao pesquisar clientes por termo:");
            System.out.println(e.getMessage());
        }

        return clientes;
    }

    public List<Cliente> listarTodos() throws SQLException {

        List<Cliente> clientes = new ArrayList<>();

        String sql = """
            SELECT
                id_cliente,
                nome_cliente,
                data_nascimento,
                cpf
            FROM cliente
            ORDER BY id_cliente;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()
        ) {
            while (resultado.next()) {

                int id = resultado.getInt("id_cliente");
                String nome = resultado.getString("nome_cliente");
                String dataNascimento = resultado.getString("data_nascimento");
                String cpf = resultado.getString("cpf");

                Cliente cliente = new Cliente(
                        id,
                        nome,
                        parseDataNascimento(dataNascimento),
                        cpf
                );

                clientes.add(cliente);
            }
        }

        return clientes;
    }

    public boolean atualizar(Cliente cliente) throws SQLException {

        String sql = """
            UPDATE cliente
            SET
                nome_cliente = ?,
                data_nascimento = ?,
                cpf = ?
            WHERE id_cliente = ?;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setString(1, cliente.getNomeCliente());
            comando.setString(2, cliente.getDataNascimento() != null ? cliente.getDataNascimento().format(Cliente.FORMATO_DATA) : "");
            comando.setString(3, cliente.getCpf());
            comando.setInt(4, cliente.getIdCliente());

            int linhasAlteradas = comando.executeUpdate();

            return linhasAlteradas > 0;
        }
    }

    public boolean deletar(int idCliente) throws SQLException {

        String sql = """
            DELETE FROM cliente
            WHERE id_cliente = ?;
            """;

        try (
                Connection conexao = BancoDeDados.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setInt(1, idCliente);

            int linhasAlteradas = comando.executeUpdate();

            return linhasAlteradas > 0;
        }
    }
}
