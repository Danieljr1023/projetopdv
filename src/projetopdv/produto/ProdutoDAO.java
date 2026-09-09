package projetopdv.produto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import projetopdv.dados.BancoDeDados;

public class ProdutoDAO {

    public Produto cadastrar(
            String nomeProduto,
            String codBarras,
            BigDecimal precoVenda) {

        // Validação defensiva das regras de domínio antes de persistir no banco
        new Produto(1, nomeProduto, codBarras, precoVenda);

        String sql = """
            INSERT INTO produto
                (nome_produto, codigo_barras, preco_centavos)
            VALUES (?, ?, ?);
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            long precoCentavos = precoVenda
                    .movePointRight(2)
                    .longValueExact();

            comando.setString(1, nomeProduto);
            comando.setString(2, codBarras);
            comando.setLong(3, precoCentavos);

            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {

                if (chaves.next()) {

                    int idProduto = chaves.getInt(1);

                    return new Produto(
                            idProduto,
                            nomeProduto,
                            codBarras,
                            precoVenda
                    );
                }
            }

            throw new SQLException(
                    "O banco não retornou o ID do produto."
            );

        } catch (SQLException | ArithmeticException e) {

            System.out.println("Erro ao cadastrar produto:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public Produto buscarPorId(int idProduto) {

        String sql = """
            SELECT
                id_produto,
                codigo_barras,
                nome_produto,
                preco_centavos
            FROM produto
            WHERE id_produto = ?;
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(sql)
        ) {

            comando.setInt(1, idProduto);

            try (ResultSet resultado = comando.executeQuery()) {

                if (resultado.next()) {

                    int id = resultado.getInt("id_produto");
                    String codigoBarras = resultado.getString("codigo_barras");
                    String nome = resultado.getString("nome_produto");
                    long precoCentavos = resultado.getLong("preco_centavos");
                    BigDecimal preco = BigDecimal.valueOf(precoCentavos, 2);

                    return new Produto(
                            id,
                            nome,
                            codigoBarras,
                            preco
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println("Erro ao consultar produto:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public Produto buscarPorCodBarras(String codBarras) {
        if (codBarras == null || codBarras.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT
                id_produto,
                codigo_barras,
                nome_produto,
                preco_centavos
            FROM produto
            WHERE codigo_barras = ?;
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(sql)
        ) {
            comando.setString(1, codBarras.trim());

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    int id = resultado.getInt("id_produto");
                    String codigoBarras = resultado.getString("codigo_barras");
                    String nome = resultado.getString("nome_produto");
                    long precoCentavos = resultado.getLong("preco_centavos");
                    BigDecimal preco = BigDecimal.valueOf(precoCentavos, 2);

                    return new Produto(
                            id,
                            nome,
                            codigoBarras,
                            preco
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar produto por código de barras:");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public List<Produto> pesquisarPorTermo(String termo) {
        List<Produto> produtos = new ArrayList<>();
        if (termo == null || termo.trim().isEmpty()) {
            return produtos;
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
                id_produto,
                codigo_barras,
                nome_produto,
                preco_centavos
            FROM produto
            WHERE LOWER(nome_produto) LIKE LOWER(?) OR codigo_barras LIKE ?
            ORDER BY nome_produto;
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(sql)
        ) {
            comando.setString(1, padraoLike);
            comando.setString(2, padraoLike);

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    int id = resultado.getInt("id_produto");
                    String codigoBarras = resultado.getString("codigo_barras");
                    String nome = resultado.getString("nome_produto");
                    long precoCentavos = resultado.getLong("preco_centavos");
                    BigDecimal preco = BigDecimal.valueOf(precoCentavos, 2);

                    produtos.add(new Produto(id, nome, codigoBarras, preco));
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao pesquisar produtos por termo:");
            System.out.println(e.getMessage());
        }

        return produtos;
    }

    public List<Produto> listarTodos() throws SQLException {

        List<Produto> produtos = new ArrayList<>();

        String sql = """
            SELECT
                id_produto,
                codigo_barras,
                nome_produto,
                preco_centavos
            FROM produto
            ORDER BY id_produto;
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()
        ) {
            while (resultado.next()) {

                int id = resultado.getInt("id_produto");
                String codigoBarras = resultado.getString("codigo_barras");
                String nome = resultado.getString("nome_produto");
                long precoCentavos = resultado.getLong("preco_centavos");
                BigDecimal preco = BigDecimal.valueOf(precoCentavos, 2);

                Produto produto = new Produto(
                        id,
                        nome,
                        codigoBarras,
                        preco
                );

                produtos.add(produto);
            }
        }

        return produtos;
    }

    public boolean atualizar(Produto produto) throws SQLException {

        String sql = """
            UPDATE produto
            SET
                nome_produto = ?,
                codigo_barras = ?,
                preco_centavos = ?
            WHERE id_produto = ?;
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(sql)
        ) {

            long precoCentavos = produto
                    .getPrecoVenda()
                    .movePointRight(2)
                    .longValueExact();

            comando.setString(1, produto.getNomeProduto());
            comando.setString(2, produto.getCodBarras());
            comando.setLong(3, precoCentavos);
            comando.setInt(4, produto.getIdProduto());

            int linhasAlteradas = comando.executeUpdate();

            return linhasAlteradas > 0;
        }
    }

    public boolean deletar(int idProduto) throws SQLException {

        String sql = """
            DELETE FROM produto
            WHERE id_produto = ?;
            """;

        try (
                Connection conexaoProduto = BancoDeDados.conectar();
                PreparedStatement comando = conexaoProduto.prepareStatement(sql)
        ) {

            comando.setInt(1, idProduto);

            int linhasAlteradas = comando.executeUpdate();

            return linhasAlteradas > 0;
        }
    }
}
